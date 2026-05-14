---
title: Android and iOS Development
---

# Android and iOS Development

## Android

Основной локальный путь:

```bash
./gradlew :composeApp:assembleDebug
```

Для быстрой проверки CI-подобного пути:

```bash
./gradlew :composeApp:lintDebug
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:clean :composeApp:assembleDebug
```

## iOS

Минимальная compile-проверка shared target:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Для UI-запуска:

- открыть `iosApp/` в Xcode;
- выбрать simulator;
- запустить host app.

## Network debug

В приложении есть `NetworkDebugScreen`, который позволяет:

- посмотреть текущий base URL;
- увидеть, идет ли работа через local API;
- задать manual host IP override.
