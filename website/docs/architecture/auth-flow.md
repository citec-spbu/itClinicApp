---
title: Auth Flow
---

# Auth Flow

## Компоненты

- `OnboardingScreen`
- `MobileAuthApi`
- `AuthManager`
- `AuthInterceptor`
- local preferences storage

## Поток

1. UI открывает GitHub auth URL.
2. Auth backend возвращает authorization code.
3. Клиент обменивает code на `MobileAuthSession`.
4. `AuthManager` получает access token.
5. Персистентное хранилище держит session data между запусками.
6. `AuthInterceptor` подставляет `Authorization: Bearer ...` в защищенные запросы.

## Почему это важно

Старый подход с `setTestToken()` все еще существует как dev helper в коде, но он не описывает основной production flow и не должен быть ядром документации.
