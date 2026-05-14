---
title: Onboarding Screen
---

# Onboarding Screen

## Назначение

Экран onboarding показывает первый выбор режима работы:

- войти через GitHub;
- продолжить без авторизации.

## Когда показывается

- при первом запуске приложения;
- пока локальный флаг завершения onboarding не сохранен в `AppPreferences`.

## Дизайн

- полноэкранный белый фон;
- большой фоновой логотип СПбГУ;
- в верхней части название `CiteC` шрифтом `Philosopher-Bold`;
- в центре большая GitHub-кнопка;
- ниже компактная текстовая кнопка `Продолжить без авторизации`;
- внизу отображается версия приложения, если она доступна.

## Важные параметры UI

- GitHub button width: `261dp`
- GitHub button height: `55dp`
- button shape: `RoundedCornerShape(25dp)`
- заголовок: `40sp`
- нижняя secondary action оформлена как подчеркнутая текстовая кнопка

## Навигация

- `Login with GitHub` открывает внешний auth URL;
- `Continue without auth` завершает onboarding локально и переводит пользователя в основной tab shell.

## Состояния

- исходный экран без ошибки;
- переход во внешний OAuth flow;
- переход в гостевой режим.

## Данные и хранилище

- `onboarding_completed` сохраняется локально через `AppPreferences`;
- строки экрана берутся из `LocalAppStrings`;
- версия приложения читается через platform abstraction.

## Сетевые запросы

Сам `OnboardingScreen` запросы не делает, но GitHub-вход ведет в auth flow:

- `GET /mobile/githubauthenticate`
- `POST /mobile/exchange`
- `POST /mobile/session`

Эти запросы реализованы в `MobileAuthApi`.
