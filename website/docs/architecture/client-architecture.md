---
title: Client Architecture
---

# Client Architecture

## Общая схема

Клиент организован вокруг KMP-модуля `composeApp`:

- `commonMain` — большая часть бизнес-логики, моделей, networking и Compose UI;
- `androidMain` — Android-specific integration;
- `iosMain` — iOS-specific integration;
- `iosApp` — Swift host app для iOS.

## Основные домены

- `core/` — настройки, auth, networking, platform integration;
- `projects/` — проекты и project detail;
- `rating/` — ranking, statistics, export и detail screens;
- `user/` — профиль и user-related API.

## Dependency wiring

`DependencyContainer` создает API-клиенты, repositories и view models. Это не DI framework, а компактный composition root.

## Навигация

Main tabs живут отдельно от overlay stack, поэтому detail/statistics flows открываются поверх tab shell.
