---
title: Project Statistics
---

# Project Statistics

## Что показывает экран

Project statistics screen собирает проектный snapshot по выбранному репозиторию и периоду и показывает:

- commits;
- issues;
- pull requests;
- rapid pull requests;
- code churn;
- code ownership;
- dominant weekday.

## Что можно менять

- активный repository;
- диапазон дат;
- rapid PR threshold;
- состав видимых секций через settings screen.

## Detail screens

Каждый блок может открывать detail screen с более сырой статистикой. Важно понимать, что detail screens строятся клиентом из уже загруженных snapshot-данных, а не из отдельного стабильного backend contract.

## Экспорт

Project statistics поддерживает export. Export payload собирается из текущего `ProjectStatsUiModel`, а не из отдельного server-side reporting endpoint.

## Где смотреть код

- `rating/presentation/projectstats/ProjectStatsScreen.kt`
- `rating/presentation/projectstats/ProjectStatsViewModel.kt`
- `rating/data/repository/ProjectStatsRepository.kt`
- `rating/data/model/ProjectStatsModels.kt`
