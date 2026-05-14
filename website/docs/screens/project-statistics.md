---
title: Project Statistics Screen
---

# Project Statistics Screen

## Назначение

Показывает сводную статистику по проекту и сравнение участников команды.

## Точка входа

Экран открывается из project detail по действию `Статистика проекта`.

## Дизайн

Состав экрана:

1. top bar и название проекта;
2. блок с заказчиком и участниками;
3. repository selector;
4. date range selector;
5. metric cards;
6. detail overlays;
7. footer actions для settings/export/ranking.

## Метрики

- commits
- issues
- pull requests
- rapid pull requests
- code churn
- code ownership
- dominant weekday

## Настраиваемые параметры

- repository;
- date range;
- rapid PR threshold;
- набор отображаемых секций через settings screen.

## Состояния

- `Loading`
- `Success(data: ProjectStatsUiModel)`
- `Error(message)`

Также есть отдельный `isRefreshing`.

## Данные

`ProjectStatsUiModel` содержит:

- project metadata;
- repositories;
- current date range;
- metric sections;
- detail payload;
- threshold state;
- export-related information.

## Запросы

Экран использует metric backend через `ProjectStatsRepository` и `MetricApi`:

- `GET /project`
- `GET /project/{id}`

Для peer ranking context клиент также может подтягивать snapshot-данные соседних проектов.

## Дополнительные действия

- открытие статистики участника;
- detail screens по секциям;
- export PDF;
- export Excel;
- открытие общего ranking;
- настройка порядка и видимости секций.
