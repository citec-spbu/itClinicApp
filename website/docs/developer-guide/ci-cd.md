---
title: CI/CD
---

# CI/CD

## Overview

Этот репозиторий рассматривается как frontend-only Kotlin Multiplatform проект.

- единственный Gradle module в репозитории: `:composeApp`;
- CI валидирует Android app и shared KMP code;
- backend checkout не требуется для обычной CI-сборки клиента;
- для сборки используются временные stub config files вместо локальных developer secrets.

## Workflow

Главный workflow:

- `.github/workflows/mobile-app-ci-cd.yml`

В нем четыре job:

- `Android CI`
- `iOS Kotlin Validation`
- `Publish App Showcase Image`
- `Publish Android Update Release`

Отдельный workflow для документации:

- `.github/workflows/docs-site-pages.yml`

Он:

- собирает Docusaurus site из `website/`;
- публикует `website/build` в GitHub Pages;
- срабатывает на push в `main` при изменениях в `website/**`, `README.md` или самом workflow.

Production URL документации:

- `https://citec-spbu.github.io/itClinicApp/`

## Pull Requests

На pull request запускается `Android CI` на `ubuntu-latest`.

Шаги:

- `./gradlew :composeApp:lintDebug --stacktrace`
- `./gradlew :composeApp:testDebugUnitTest --stacktrace`
- `./gradlew :composeApp:clean :composeApp:assembleDebug`

Артефакты:

- Android lint reports;
- Android unit test reports;
- delivery artifacts из `release-artifacts/`.

## Main and develop

Push в default branch продолжает запускать `Android CI`.

Push в default branch и `develop` также запускает:

- `iOS Kotlin Validation`

Эта job выполняет:

- `./gradlew :composeApp:compileKotlinIosSimulatorArm64 --stacktrace`

Это держит KMP iOS target компилируемым без code signing, Xcode automation и backend runtime.

Push в default branch дополнительно запускает:

- `Publish App Showcase Image`
- `Publish Android Update Release`

## Tags and releases

Теги вида `v*` запускают:

- `Android CI`
- `iOS Kotlin Validation`
- `Publish App Showcase Image`
- `Publish Android Update Release`

Release job создает или обновляет GitHub Release для тега и публикует:

- `release-artifacts/itclinicapp-release.apk`
- `release-artifacts/android-update.json`
- `stable.json` channel manifest в ветку `android-updates`

## Android update delivery

CD-путь публикует:

- signed release APK;
- `android-update.json`;
- channel manifest (`beta.json` или `stable.json`);
- rolling prerelease для main builds или полноценный tagged release.

Android-клиент:

- считывает manifest для канала;
- сравнивает `versionCode`;
- скачивает APK;
- проверяет SHA-256 checksum;
- передает файл системному инсталлеру.

## GHCR showcase image

Image публикуется как:

- `ghcr.io/<owner>/itclinicapp-showcase`

Image содержит:

- lightweight static landing page;
- CI-собранный APK;
- `android-update.json`.

## Secrets and variables

Обязательные GitHub Secrets:

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

Используемый built-in token:

- `GITHUB_TOKEN`

Он нужен для:

- публикации GHCR image;
- создания и обновления GitHub Releases;
- загрузки release assets;
- обновления ветки `android-updates`.

Опциональные GitHub Variables:

- `ANDROID_BETA_MIN_SUPPORTED_VERSION_CODE`
- `ANDROID_BETA_FORCE_UPDATE`
- `ANDROID_STABLE_MIN_SUPPORTED_VERSION_CODE`
- `ANDROID_STABLE_FORCE_UPDATE`

## CI helper scripts

Workflow опирается на:

- `scripts/prepare_ci_stubs.sh`
- `scripts/prepare_mobile_showcase.sh`
- `scripts/generate_android_update_manifest.sh`
- `scripts/resolve_android_update_changelog.sh`

Поведение changelog:

- если `release-notes/android-update-notes.txt` изменился относительно `HEAD^`, каждая непустая строка становится changelog item;
- пустые строки и строки с `#` игнорируются;
- если файл не менялся в текущем commit, published changelog будет пустым.

## Local commands that mirror CI

```bash
./gradlew :composeApp:lintDebug
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:clean :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Local config in CI context

Ожидаемые локальные файлы:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.kt`

Они:

- игнорируются Git;
- принадлежат разработчику;
- не должны быть обязательны для прохождения CI.

Шаблоны:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.example.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.example.kt`

Важное поведение scripts:

- `prepare_ci_stubs.sh` не должен перетирать локальные конфиги вне CI;
- `prepare_mobile_showcase.sh` временно генерирует CI-safe stubs для artifact preparation и затем восстанавливает существующие локальные конфиги.

## Docker image usage

Pull:

```bash
docker pull ghcr.io/<owner>/itclinicapp-showcase:latest
```

Run:

```bash
docker run --rm -p 8080:80 ghcr.io/<owner>/itclinicapp-showcase:latest
```

Специфичный tag:

```bash
docker run --rm -p 8080:80 ghcr.io/<owner>/itclinicapp-showcase:<tag>
```

После старта контейнера доступны:

- `http://localhost:8080/`
- `http://localhost:8080/release-artifacts/itclinicapp-release.apk`
- `http://localhost:8080/release-artifacts/android-update.json`

Если GHCR package private:

```bash
echo <PAT> | docker login ghcr.io -u <github_username> --password-stdin
```

Токену нужен scope:

- `read:packages`

## Troubleshooting

### Missing `BuildConfig` or `MailConfig`

Если сборка в CI падает с unresolved references на `BuildConfig` или `MailConfig`, проверьте:

- `scripts/prepare_ci_stubs.sh`
- step `Prepare CI stubs` в workflow

### Duplicate dex classes during `assembleDebug`

Если Android packaging падает на duplicate dex entries вроде `*.dex` и `* 2.dex`, локальный build directory stale.

Используйте:

```bash
./gradlew :composeApp:clean :composeApp:assembleDebug
```

### GHCR `unauthorized`

Типовые причины:

- image еще не публиковался;
- GHCR package private.

### GHCR `no matching manifest for linux/arm64/v8`

Новые публикации поддерживают:

- `linux/amd64`
- `linux/arm64`

Если старый tag не тянется на Apple Silicon, скорее всего он был выпущен до multi-arch support.

### Android in-app update check does not show anything

Проверьте, что CD действительно опубликовал:

- `itclinicapp-release.apk`
- `android-update.json`
- нужный channel manifest в `android-updates`

Также важно:

- repo должен быть публично читаемым для anonymous checks;
- versionCode установленного APK должен быть ниже manifest versionCode;
- update checks throttled и не дергают GitHub на каждой recomposition;
- update APK должен быть подписан тем же release keystore.

## Frontend-only limitations

- workflow не клонирует и не собирает backend repository;
- backend containers не нужны для CI;
- APK из CI собран со stub config, а не с developer secrets;
- iOS artifacts не превращаются в signed app bundle или TestFlight package;
- Android updates завязаны на GitHub metadata, а не на отдельный backend.

## Manual setup that still remains

- Android release signing
- Play Store publishing
- iOS code signing and distribution
- making GHCR package public if needed
- keeping repository or release channel public for anonymous Android update checks
- replacing CI stub config with real runtime config в local developer environments
