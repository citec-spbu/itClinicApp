---
title: Projects and Navigation
---

# Projects and Navigation

## Главные разделы

После onboarding пользователь попадает в основной shell приложения с тремя вкладками:

- `Projects`
- `Ranking / Statistics`
- `Information`

## Projects

Во вкладке проектов доступны:

- список проектов;
- фильтрация;
- карточка проекта;
- сценарий `My Project`, если пользователь участвует в проекте.

## Переходы

Из карточки проекта можно перейти:

- в детальный экран проекта;
- в статистику проекта;
- в статистику конкретного участника.

## Отдельный overlay stack

Поверх main tabs приложение держит стек экранов для detail/statistics flows. Это позволяет:

- не терять выбранную вкладку;
- открывать nested screens поверх основного UI;
- по-разному анимировать stats overlays и обычные overlays.

## Где смотреть код

- `main/presentation/MainScreen.kt`
- `projects/presentation/ProjectsScreen.kt`
- `projects/presentation/detail/ProjectDetailScreen.kt`
- `App.android.kt`
- `App.ios.kt`
