---
title: Update Delivery
---

# Update Delivery

## Что реализовано

Android-клиент умеет проверять обновления через GitHub-публикацию, а не через отдельный backend.

## Источники данных

- GitHub Release assets
- ветка `android-updates`
- channel manifests (`beta.json`, `stable.json`)

## Что делает клиент

Клиент:

- сравнивает текущую и доступную версии;
- скачивает APK;
- проверяет SHA-256 checksum;
- передает файл системному инсталлеру.

## Где смотреть код

- `core/update/AndroidAppUpdateChecker.kt`
- `core/update/AndroidAppUpdateInstaller.kt`
- `.github/workflows/mobile-app-ci-cd.yml`
