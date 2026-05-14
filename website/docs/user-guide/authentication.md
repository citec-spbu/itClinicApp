---
title: Authentication
---

# Authentication

## Режимы работы

Приложение поддерживает два режима:

- вход через GitHub;
- продолжение без авторизации.

## Как устроен вход сейчас

На onboarding screen кнопка GitHub не подставляет локальный тестовый JWT. Вместо этого она открывает мобильный GitHub OAuth flow через auth backend.

Практически это выглядит так:

1. Пользователь нажимает `Login with GitHub`.
2. Приложение открывает внешний auth URL.
3. Auth backend возвращает код.
4. Клиент обменивает код на `accessToken` и `refreshToken`.
5. Сессия сохраняется локально и используется для последующих запросов.

## Что доступно без авторизации

Гостевой режим нужен для ограниченного просмотра приложения без auth session.

Типичные ограничения:

- меньше доступных действий на защищенных экранах;
- часть project/user flows зависит от токена;
- ranking/statistics сценарии ориентированы на авторизованного пользователя.

## Где искать реализацию

- onboarding UI: `onboarding/presentation/OnboardingScreen.kt`
- запуск OAuth: `App.android.kt`, `App.ios.kt`
- auth API: `core/auth/MobileAuthApi.kt`
- текущее состояние auth: `core/auth/AuthManager.kt`
- добавление токена к запросам: `core/network/AuthInterceptor.kt`
