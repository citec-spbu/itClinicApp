---
title: Local Config
---

# Local Config

## Локальные файлы конфигурации

Ожидаются два локальных файла:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.kt`

Оба файла не должны коммититься.

## Шаблоны

Шаблоны лежат здесь:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.example.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.example.kt`

## Что задается в BuildConfig

- `USE_LOCAL_API`
- `PRODUCTION_BASE_URL`
- `AUTH_PRODUCTION_BASE_URL`
- `LOCAL_PORT`
- `AUTH_LOCAL_PORT`
- `LOCAL_HOST_IP`
- `METRIC_PRODUCTION_BASE_URL`
- `METRIC_LOCAL_PORT`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

## Что задается в MailConfig

SMTP-параметры для feedback flow.

Если feedback не нужен в локальном сценарии, можно оставить технически валидный, но не production-ready конфиг.

## Переключение окружения

Переключение local/prod API делается через `BuildConfig.USE_LOCAL_API`. `ApiConfig.kt`, `AuthApiConfig.kt` и `MetricApiConfig.kt` читают этот флаг и строят base URL автоматически.
