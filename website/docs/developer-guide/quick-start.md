---
title: Quick Start
---

# Quick Start

## Что нужно для старта

- JDK 17+
- Android Studio
- Xcode для iOS
- Node.js для docs-site

## Базовый путь

1. Создать локальные `BuildConfig.kt` и `MailConfig.kt` из шаблонов.
2. Решить, нужен ли вам локальный backend stack из [Registry](https://github.com/AngW1SH/Registry).
3. Собрать Android debug build.
4. При необходимости проверить iOS Kotlin compilation.

## Минимальные команды

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Что важно понять заранее

- backend не является Gradle-модулем мобильного проекта;
- часть локальной разработки зависит от внешнего backend-репозитория [Registry](https://github.com/AngW1SH/Registry) и его compose stack;
- CI умеет собирать проект на stub config без developer secrets;
- документация сайта живет в `website/`.
