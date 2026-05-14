---
title: Current Status
---

# Current Status

## Что уже есть в коде

- GitHub OAuth для мобильного входа;
- русский и английский языки интерфейса;
- light / dark / system theme mode;
- проектная и личная статистика на клиенте;
- Android update delivery через GitHub Releases и ветку `android-updates`;
- CI/CD для Android и compile-check iOS Kotlin target.

## Что еще не закрыто полностью

- часть статистики и detail screens живет на client-side агрегации поверх snapshot-данных;
- feedback сейчас отправляется из клиента через SMTP-конфиг;
- backend contracts для ranking/stats остаются частично нестабильными.

## Подход к этой новой доке

Цель сайта:

- укрупнить знания по темам, а не по каждому экрану;
- убрать дубли;
- описывать реальные flow и зависимости;
- явно помечать backend gaps и client-side workarounds.
