# Техническая документация

## Архитектура

Проект состоит из трех Maven-модулей:

- `shared` - общая библиотека с моделями, репозиториями, сервисами и Flyway-миграциями.
- `studentbot` - Telegram long polling bot для студентов.
- `adminweb` - Spring Boot web backend и React frontend админки.

База данных - PostgreSQL. Схема создается Flyway-миграциями из `shared/src/main/resources/db/migration`.

## Backend

Основные технологии:

- Java 21
- Spring Boot 3.3
- Spring JDBC
- Spring Security для web-админки
- Flyway
- Apache POI для импорта Excel
- Telegram Bots Java API

Важные слои:

- `shared/repository` - SQL-доступ к данным.
- `shared/service` - файловые операции, импорт, AI-проверка, уведомления, тестовая логика.
- `adminweb/api` - REST endpoints для админки.
- `studentbot/handler` - обработчики сценариев Telegram-бота.

## Frontend

Frontend находится в `adminweb/frontend`.

Основные технологии:

- React
- TypeScript
- Vite
- Ant Design
- Axios
- Vitest + Testing Library

Язык интерфейса админки - узбекский. Новые UI-строки должны быть на узбекском языке.

## Конфигурация

Минимальные переменные:

- `STUDENT_BOT_TOKEN` - токен Telegram-бота студента.
- `ADMIN_PASSWORD` - пароль входа в web admin.
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` - настройки PostgreSQL.
- `APP_MATERIALS_PATH` или Docker volume `/app/storage/materials` - путь к учебным файлам.

AI-проверка ситуационных заданий:

- `GRADING_API_URL`
- `GRADING_API_KEY`
- `GRADING_MODEL`

Если AI-ключ не настроен, ручная проверка ситуационных заданий должна работать.

## Хранилище файлов

Учебные материалы сохраняются внутри `storage/materials`. `FileService` должен защищать операции от path traversal. Нельзя использовать пользовательский путь напрямую без нормализации и проверки через корневую директорию.

## Сборка

Backend без frontend npm:

```powershell
mvn package "-Dskip.npm=true"
```

Полная Maven-сборка может запускать frontend plugin, но локально быстрее проверять frontend отдельно:

```powershell
cd adminweb/frontend
npm.cmd install
npm.cmd run build
```

## Docker

`docker-compose.yml` поднимает:

- `postgres`
- `studentbot`
- `adminweb`

Оба приложения используют общий volume `./storage/materials:/app/storage/materials`.
