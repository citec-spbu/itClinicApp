---
title: API Endpoints
---

# API Endpoints

Этот раздел описывает endpoint'ы, которые прямо зашиты в текущем мобильном клиенте. Он не претендует на полную backend-спецификацию.

## Public API

Основной API:

- `GET /project/active`
- `GET /project/new`
- `POST /project/findmany`
- `GET /project/{slug}`
- `GET /user/role`
- `GET /tag`
- `POST /email`

## Auth-required API

- `GET /user/projectstatus`
- `GET /user/profile`
- `GET /user`
- `POST /request`
- `PUT /request`
- `GET /request/available`
- `DELETE /request/{id}`
- `POST /project/results/change-file`
- `DELETE /project/results/delete-file`
- `POST /project/results/upload-file`
- `POST /project/links`
- `DELETE /project/links/{id}`
- `PUT /profile/account`
- `PUT /profile/personal`
- `PUT /member`

## Mobile auth API

Отдельный auth backend используется для mobile auth flow:

- `GET /mobile/githubauthenticate`
- `POST /mobile/exchange`
- `POST /mobile/session`
- `GET /logout`

## Источник правды

Актуальные endpoint-строки и URL-правила задаются в:

- `core/network/ApiConfig.kt`
- `core/network/AuthApiConfig.kt`
- `core/auth/MobileAuthApi.kt`
