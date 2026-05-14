---
title: Networking
---

# Networking

## Базовые конфиги

Клиент использует три конфигурации base URL:

- основной API: `ApiConfig`
- auth API: `AuthApiConfig`
- metric API: `MetricApiConfig`

Все три читают флаги и адреса из `BuildConfig`.

## Local vs production

Если `USE_LOCAL_API = true`, клиент строит local URL с учетом:

- platform-specific host resolution;
- manual host override через `NetworkSettings`;
- отдельных портов для main/auth/metric API.

Если `USE_LOCAL_API = false`, клиент идет в production URLs из `BuildConfig`.

## Platform specifics

- Android emulator использует `10.0.2.2`
- iOS simulator использует `127.0.0.1`
- реальные устройства зависят от host IP resolution и manual override

## Debugging

`NetworkDebugScreen` — основной встроенный инструмент для сетевой диагностики на клиенте.
