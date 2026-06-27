# Тестирование

## Backend

Запуск:

```powershell
mvn test "-Dskip.npm=true"
```

Полная package-проверка:

```powershell
mvn package "-Dskip.npm=true"
```

Покрытые зоны:

- `StudentRepository` - серверная пагинация и поиск по ФИО/ID/Telegram ID.
- `ResultRepository` - фильтры тестов и ситуационных заданий.
- `FileService` - защита от path traversal.
- `ImportService` - импорт CSV и валидация правильного ответа.
- `StudentController` - передача фильтров и пагинации.
- `GradingController` - ручная оценка, допустимый диапазон баллов, `gradedBy = null` для web admin.

## Frontend

Запуск:

```powershell
cd adminweb/frontend
npm.cmd run test
npm.cmd run build
npm.cmd run lint
```

Покрытые зоны:

- страница `Talabalar`;
- наличие фильтров поиска;
- отправка серверного запроса с `page`, `size`, `name`, `group`, `subgroup`;
- защита от сценария, где фронт должен был бы грузить полный список студентов.

## Что стоит добавить дальше

- Integration tests с PostgreSQL/Testcontainers для миграций и реальных SQL-запросов.
- API tests для `ContentPage` endpoints.
- Frontend tests для `TestResultsPage`, `SituationalPage`, `ContentPage`.
- E2E tests через Playwright для основных админских сценариев.

## Ожидаемый минимум перед релизом

Перед выкладкой должны проходить:

```powershell
mvn package "-Dskip.npm=true"
cd adminweb/frontend
npm.cmd run test
npm.cmd run build
npm.cmd run lint
```

Vite warning о размере bundle сейчас допустим, но его нужно закрыть позже через code splitting.
