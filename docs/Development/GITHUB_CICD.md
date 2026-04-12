# GitHub Actions CI/CD

## Scope

Этот workflow настроен только для мобильного приложения в этом репозитории.

- backend не требуется
- CI проверяет Android/Compose слой
- CD публикует не backend-контейнер, а showcase-образ с документацией проекта

Основной workflow: `.github/workflows/mobile-app-ci-cd.yml`

## Что делает workflow

### CI

Выполняются два job:

- `Android Lint`
  - команда: `./gradlew :composeApp:lintDebug --stacktrace`
- `Android Unit Tests`
  - команда: `./gradlew :composeApp:testDebugUnitTest --stacktrace`

Перед запуском workflow автоматически создаёт временные файлы:

- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/BuildConfig.kt`
- `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/MailConfig.kt`

Они генерируются из:

- `BuildConfig.example.kt`
- `MailConfig.example.kt`

Это нужно потому, что реальные локальные конфиги не коммитятся в Git.

### CD

При `push` в default branch или при push тега workflow публикует Docker-образ:

- `ghcr.io/<owner>/itclinicapp-showcase`

В образ попадают:

- статическая стартовая страница
- `README.md`
- папка `docs/`

Контейнер использует `nginx`.

## Теги образов

При публикации создаются immutable tags:

- короткий SHA коммита
- tag ветки
- git tag, если публикация идёт по release tag

Дополнительно:

- `latest` обновляется только для default branch

Идея простая: `latest` это лишь указатель, а не единственный источник правды.

## Стратегия отката

Откат делается без новой сборки.

1. Откройте `Actions`
2. Выберите workflow `Mobile App CI/CD`
3. Нажмите `Run workflow`
4. Укажите `rollback_tag`
5. Запустите workflow вручную

Rollback job выполнит promotion уже существующего образа:

- из `ghcr.io/<owner>/itclinicapp-showcase:<rollback_tag>`
- в `ghcr.io/<owner>/itclinicapp-showcase:latest`

То есть откат меняет только указатель `latest`.

## Почему `docker pull ghcr.io/...:latest` может вернуть `unauthorized`

Обычно причина одна из двух:

1. образ ещё не был опубликован
2. пакет в GHCR приватный, а клиент не залогинен

### Сценарий 1: образ не опубликован

Если `Android Lint` или `Android Unit Tests` падают, publish job не запускается.

Тогда:

- `latest` не создаётся
- `docker pull` не сможет получить образ

Сначала нужно добиться зелёного workflow.

### Сценарий 2: GHCR package приватный

По умолчанию package в GitHub Container Registry часто приватный.

Тогда для pull нужен логин:

```bash
echo <PAT> | docker login ghcr.io -u <github_username> --password-stdin
docker pull ghcr.io/<owner>/itclinicapp-showcase:latest
```

Для `PAT` нужен scope:

- `read:packages`

Если pull должен работать без логина:

1. откройте package в GitHub
2. переключите visibility на `public`

## Что проверить после push

1. Во вкладке `Actions` должен появиться workflow `Mobile App CI/CD`
2. Должны пройти:
   - `Android Lint`
   - `Android Unit Tests`
3. Для default branch или тега должен стартовать:
   - `Publish App Showcase Image`
4. В `Packages` должен появиться пакет:
   - `itclinicapp-showcase`

## EN Summary

This repository uses an app-only GitHub Actions workflow.

- CI runs `lintDebug` and `testDebugUnitTest`
- CD publishes `ghcr.io/<owner>/itclinicapp-showcase`
- rollback is performed by promoting an existing immutable tag back to `latest`
- `unauthorized` on `docker pull` usually means either the image was never published or the GHCR package is private
