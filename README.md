# Dentistry Bot Platform

Платформа для учебного процесса по стоматологии: студент работает через Telegram-бота, администратор управляет контентом и проверкой через веб-панель.

## Состав проекта

- `studentbot/` - Telegram-бот для студентов.
- `adminweb/` - Spring Boot backend и React frontend админ-панели.
- `shared/` - общие модели, репозитории, сервисы, миграции БД.
- `configs/` - внешние конфиги, включая prompt для AI-проверки.
- `storage/materials/` - локальное хранилище учебных файлов.
- `docs/` - подробная документация по технической части и бизнес-логике.

## Главное решение

Старый Telegram-бот администратора удален. Админский функционал должен выполняться только через web UI. Старый `oldREADME.md` удален, потому что описывал уже неактуальную архитектуру.

## Быстрый запуск

1. Создать `.env` на основе `.env.example`.
2. Указать `STUDENT_BOT_TOKEN` и `ADMIN_PASSWORD`.
3. Запустить:

```powershell
docker compose up --build
```

Админка будет доступна на `http://localhost:8080`.

## Проверки

Backend:

```powershell
mvn package "-Dskip.npm=true"
```

Frontend:

```powershell
cd adminweb/frontend
npm.cmd run test
npm.cmd run build
npm.cmd run lint
```

## Документация

- [Техническая документация](docs/TECHNICAL.md)
- [Бизнес-логика](docs/BUSINESS_LOGIC.md)
- [Админская веб-панель](docs/ADMIN_WEB.md)
- [Тестирование](docs/TESTING.md)
- [Ожидания и ограничения](docs/EXPECTATIONS.md)
