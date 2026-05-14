---
title: Ranking Screen
---

# Ranking Screen

## Назначение

Показывает ranking:

- проектов;
- студентов.

## Дизайн

- top title `Рейтинг`;
- переключатель `Projects / Students`;
- строка поиска;
- action icons для фильтров и сортировки;
- чипсы примененных фильтров;
- ranking list с позициями, названием и score;
- quick action на личную статистику.

## Поведение

- screen загружает ranking data при первом открытии;
- фильтры могут быть пере-применены без перезапуска экрана;
- refresh запускает force refresh;
- screen хранит tab/page scroll state локально внутри `RankingViewModel`.

## Состояния

- `Idle`
- `Loading`
- `Success(data)`
- `Error(message)`

## Данные

`RankingData` включает:

- project ratings;
- student ratings;
- выбранные фильтры;
- метаданные для UI-рендеринга score и ranking positions.

## Запросы

Ranking использует metric backend:

- `GET /project`
- `GET /project/{id}`
- `GET /rating/students`
- `POST /rating/sync` при синхронизации project snapshots

## Особенность реализации

Часть ranking logic считается на клиенте через `RankingRepository` и `RankingScoreEngine`, а не приходит готовой из одного backend endpoint.
