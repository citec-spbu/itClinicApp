---
title: Repository Map
---

# Repository Map

## Основные директории

- `composeApp/` — общий KMP-модуль с Compose UI, networking, auth, ranking, statistics и export.
- `iosApp/` — iOS host app и Xcode project.
- `website/` — документация на Docusaurus.
- `scripts/` — локальные и CI helper scripts.
- `docker/` — контейнер для APK showcase.
- `.github/workflows/` — GitHub Actions workflow.

## Полезные точки входа

- `composeApp/src/androidMain/kotlin/com/spbu/projecttrack/App.android.kt`
- `composeApp/src/iosMain/kotlin/com/spbu/projecttrack/App.ios.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/core/di/DependencyContainer.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/core/network/ApiConfig.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/core/auth/MobileAuthApi.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/rating/`

## Как читать репозиторий

Если задача продуктовая, обычно начинать стоит с:

- `main/presentation/`
- `projects/presentation/`
- `rating/presentation/`
- `core/settings/`

Если задача инфраструктурная:

- `BuildConfig.example.kt`
- `scripts/`
- `.github/workflows/mobile-app-ci-cd.yml`
