# План безопасности для публичного домена

Составлено после аудита текущего состояния сервера (`ubuntu-4gb-nbg1-3`, Hetzner). Обновлено 2026-09-01 после первой волны хардненинга.

## ✅ Уже сделано (2026-09-01)

- `ufw` включён: 22/80/443 разрешены, всё остальное блокируется по умолчанию.
- `adminweb` перепривязан на `127.0.0.1:8080` — порт больше не торчит в интернет (docker публиковал его на `0.0.0.0`, из-за чего один только `ufw` не помогал: докер сам прописывает iptables-правила в обход файрвола для опубликованных портов).
- Проверено на признаки компрометации за время, пока порт был открыт — следов взлома не найдено (см. переписку/историю сессии): чужих SSH-ключей, cron-задач, процессов, докер-образов, файлов в `/tmp` не обнаружено.
- `fail2ban` установлен и активен, джейл `sshd` уже забанил несколько реальных сканеров.
- Пароль по SSH отключён (`PasswordAuthentication no`, `KbdInteractiveAuthentication no`) — вход только по ключу.
- Ежедневный бэкап базы (`/opt/dental/backup.sh`, cron `0 3 * * *`), хранит дампы 14 дней в `/opt/dental/backups`.

## Что уже было в порядке изначально

- Root заходит по SSH только по ключу (`PermitRootLogin without-password`).
- `.env` с секретами — права `600`, только root.
- `unattended-upgrades` включён — патчи безопасности ставятся автоматически.
- Пароли админов хешируются BCrypt, есть CSRF-защита (XOR-токен, устойчив к BREACH).
- Postgres не публикуется наружу.
- Ubuntu 24.04 LTS, Docker свежий (29.7.1).

## Чего всё ещё нет

- Бэкапы лежат только на том же диске сервера — не защита от отказа самого сервера/диска. Стоит периодически скачивать их к себе или лить в S3-совместимое хранилище.
- Reverse proxy / TLS — нет, приложение торчит напрямую по HTTP (сейчас только внутри loopback, но без TLS в принципе, пока нет домена).
- Rate limiting на логин в админку — нет.
- Мониторинга/алертов (место на диске, аптайм) — нет.
- Пароли admin-аккаунтов не менялись — рекомендую сменить на всякий случай, раз панель какое-то время была открыта всему интернету (взлома не нашли, но дёшево перестраховаться).

---

## Шаг 1. Файрвол — сделать сейчас, до домена

```bash
ssh dental
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp        # SSH
ufw allow 80/tcp        # понадобится для Let's Encrypt / HTTP-редиректа
ufw allow 443/tcp       # HTTPS
ufw enable
ufw status verbose
```

Порт 8080 в этот список **не добавляем** — после шага 3 (reverse proxy) внешний трафик на него вообще не понадобится, он останется доступен только изнутри docker-сети и через ваш SSH-туннель для отладки.

Проверить, что fail2ban защищает SSH:

```bash
apt install -y fail2ban
systemctl enable --now fail2ban
```

(дефолтного джейла `sshd` достаточно для старта — apt-пакет включает его по умолчанию в Ubuntu 24.04)

Отключить парольный вход по SSH полностью (раз есть ключи):

```bash
# в /etc/ssh/sshd_config:
PasswordAuthentication no
# затем:
systemctl restart ssh
```

## Шаг 2. Домен

1. Купить домен, создать A-запись на IP сервера (`167.233.175.228`), например `dental-exam.yourdomain.uz` → сервер.
2. Дождаться распространения DNS (`dig +short dental-exam.yourdomain.uz` должен вернуть IP сервера).

## Шаг 3. Reverse proxy + TLS (Caddy)

Рекомендую **Caddy**, а не nginx+certbot вручную: он сам получает и продлевает сертификат Let's Encrypt, конфиг — несколько строк.

```bash
# на сервере
apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
apt update && apt install -y caddy
```

`/etc/caddy/Caddyfile`:

```
dental-exam.yourdomain.uz {
    reverse_proxy 127.0.0.1:8080

    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "strict-origin-when-cross-origin"
    }
}
```

Но `adminweb` сейчас публикует `8080:8080` (все интерфейсы) — под Caddy это нужно сузить до loopback, чтобы порт был виден только самому серверу, не миру:

```yaml
# docker-compose.yml, сервис adminweb
ports:
  - "127.0.0.1:8080:8080"
```

```bash
systemctl reload caddy
docker compose up -d adminweb
```

После этого `https://dental-exam.yourdomain.uz` — рабочий адрес админки, обычный `http://IP:8080` снаружи перестаёт отвечать вовсе (файрвол + biнд на loopback).

## Шаг 4. Приложение — довести Spring Boot до состояния "за прокси"

`adminweb/src/main/resources/application.properties`, добавить:

```properties
# Caddy шлёт X-Forwarded-Proto/Host — без этого Spring не поймёт, что запрос пришёл по HTTPS,
# и не проставит Secure-флаг на сессионную куку.
server.forward-headers-strategy=native
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

Rate limiting на `/api/auth/login` — простейший вариант без новых зависимостей: счётчик неудачных попыток по IP в памяти с блокировкой на N минут после 5 подряд неудач (bucket4j — более надёжный вариант, если понадобится расти дальше одного инстанса).

## Шаг 5. Секреты — сменить после переезда на HTTPS

Поскольку панель какое-то время была доступна по голому HTTP из интернета, стоит один раз, уже после того как заработает HTTPS:

- Сменить пароли всех admin-аккаунтов (`admin_users`).
- Проверить `.env` — при желании перевыпустить `POSTGRES_PASSWORD` (не критично, база наружу не торчит, но дёшево сделать).
- `GRADING_API_KEY` (OpenAI) — по сети наружу не уходил (сервер сам к OpenAI стучится), можно не трогать.

## Шаг 6. Бэкапы

Автоматических бэкапов базы сейчас нет вообще. Простой вариант — ежедневный дамп + ротация:

```bash
# /opt/dental/backup.sh
#!/bin/bash
set -e
BACKUP_DIR=/opt/dental/backups
mkdir -p "$BACKUP_DIR"
docker exec tgbot-postgres pg_dump -U tgbot dentistry_bot | gzip > "$BACKUP_DIR/db_$(date +%Y%m%d_%H%M%S).sql.gz"
find "$BACKUP_DIR" -name '*.sql.gz' -mtime +14 -delete
```

```bash
chmod +x /opt/dental/backup.sh
crontab -e
# 0 3 * * * /opt/dental/backup.sh
```

Локальные бэкапы на том же диске — не защита от отказа диска/сервера. Как минимум периодически скачивать дамп к себе (`scp`) или лить в S3-совместимое хранилище (Hetzner Object Storage, недорого).

## Шаг 7. Мониторинг (минимум)

- Внешний uptime-чекер на `https://dental-exam.yourdomain.uz/api/auth/me` — бесплатные варианты: UptimeRobot, Better Uptime (пингуют раз в 1-5 мин, шлют алерт в Telegram/почту при падении).
- Место на диске: `df -h` уже показывает 25G свободно из 38G — по мере роста фото/материалов/дампов стоит поставить простой cron-алерт при <15% свободного места.

## Порядок действий (чек-лист)

- [x] `ufw` + `fail2ban` + отключить SSH по паролю — сделано 2026-09-01
- [x] `adminweb` порт → `127.0.0.1:8080:8080` — сделано 2026-09-01
- [x] Ежедневный бэкап базы — сделано 2026-09-01 (локально; off-server копия — ещё нет)
- [ ] Купить домен, настроить A-запись
- [ ] Поставить Caddy, получить сертификат
- [ ] `server.forward-headers-strategy` + secure-куки
- [ ] Rate limiting на логин
- [ ] Сменить пароли admin-аккаунтов
- [ ] Копировать бэкапы за пределы сервера (scp/S3)
- [ ] Подключить внешний uptime-мониторинг

## Дальше по желанию (не блокирует запуск)

- 2FA для SUPER_ADMIN-аккаунтов.
- Логирование неудачных попыток входа (сейчас нигде не пишется).
- CSP-заголовок (сейчас нет — сложнее подобрать из-за inline-стилей antd, можно отложить).
- Вынести `storage/materials` (фото/файлы) под отдельную проверку типов при загрузке — сейчас ограничение только по размеру (50MB), не по содержимому файла.
