---
title: Profile, Settings and Feedback
---

# Profile, Settings and Feedback

## Profile

Профиль позволяет:

- читать профиль пользователя;
- редактировать personal fields;
- редактировать account fields;
- выходить из сессии.

## Settings

В настройках уже есть:

- выбор языка;
- выбор темы;
- privacy policy;
- служебные UI settings.

## Feedback

Feedback screen собирает пользовательское сообщение и отправляет его через SMTP-конфиг клиента. Это работает, но считается временным архитектурным компромиссом, потому что mail secrets не должны жить в мобильном приложении.

## Где смотреть код

- `core/settings/AppUiSettings.kt`
- `user/data/api/UserProfileApi.kt`
- `core/email/FeedbackMailSender.kt`
- `main/presentation/SettingsTabScreen.kt`
