---
title: User Statistics Screen
---

# User Statistics Screen

## Назначение

Показывает персональную статистику пользователя в контексте проекта и выбранного репозитория.

## Точка входа

Экран открывается:

- из ranking по студентам;
- из project statistics по участнику;
- из quick action `Личная статистика`.

## Дизайн

Состав похож на project statistics, но вместо проектной шапки показывает:

- ФИО пользователя;
- роль;
- связанный проект;
- repository selector;
- date range selector;
- набор metric cards;
- export/settings actions.

## Метрики

Используются те же основные группы, что и в project statistics:

- commits;
- issues;
- pull requests;
- rapid pull requests;
- code churn;
- code ownership;
- dominant weekday.

## Состояния

- `Loading`
- `Success(data: UserStatsUiModel)`
- `Error(message)`

## Данные

Экран строится через `UserStatsRepository` и получает:

- user identity;
- preferred project context;
- repository list;
- current date range;
- все metric sections;
- detail payload.

## Запросы

Основной слой получает snapshot-данные через metric backend:

- `GET /project`
- `GET /project/{id}`

Дальше клиент сам отфильтровывает и агрегирует user-specific данные внутри `UserStatsRepository`.
