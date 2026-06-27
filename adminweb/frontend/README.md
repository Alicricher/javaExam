# Admin Web Frontend

React/TypeScript frontend для веб-админки.

## Команды

```powershell
npm.cmd install
npm.cmd run dev
npm.cmd run test
npm.cmd run build
npm.cmd run lint
```

## Назначение

Frontend обслуживает четыре основных раздела:

- `Talabalar` - поиск студентов, просмотр результатов, изменение ФИО.
- `Test natijalari` - проверка результатов тестов.
- `Vaziyatli topshiriqlar` - проверка ситуационных заданий.
- `Kontent` - управление разделами, уроками, теорией, тестами и заданиями.

## Правила UI

- Язык интерфейса - узбекский.
- Большие таблицы работают через серверную пагинацию.
- Для больших списков должны быть фильтры и кнопка `Tozalash`.
- ID можно показывать как вспомогательную информацию, но основной UX должен строиться вокруг названий, поиска и каскадных фильтров.

## API

Axios client настроен на base URL `/api`. В dev-режиме Vite проксирует `/api` на `http://localhost:8080`.

## Тесты

Тестовый стек:

- Vitest
- Testing Library
- jsdom

Запуск:

```powershell
npm.cmd run test
```
