---
title: Ranking and Statistics
---

# Ranking and Statistics

## Главная идея

`rating/` — это не только UI экраны, а еще слой клиентской агрегации поверх metric snapshot payload.

## Основные части

- `RankingRepository`
- `RankingScoreEngine`
- `ProjectStatsRepository`
- `UserStatsRepository`
- `ProjectStatsViewModel`
- `UserStatsViewModel`

## Что делает клиент

Клиент умеет:

- локально пересчитывать ranking;
- агрегировать score по метрикам;
- собирать statistics sections;
- строить detail screens из raw snapshot data;
- готовить export payload.

## Почему раздел важный

Именно здесь сосредоточена большая часть временных backend workarounds. Любое изменение ranking/stats logic надо документировать одновременно в коде и в `Reference / Backend Gaps`.
