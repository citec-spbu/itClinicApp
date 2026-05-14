---
title: Local Backend Stack
---

# Local Backend Stack

## Для чего нужен backend repository

Локальное backend/auth/metric окружение живет в отдельном репозитории:

- [Registry](https://github.com/AngW1SH/Registry)

Мобильный клиент использует его как внешнюю зависимость в dev-сценариях, но CI мобильного репозитория не требует backend checkout для обычной сборки клиента.

## Что есть в репо

- `scripts/start_mobile_local_stack.sh`
- `scripts/stop_mobile_local_stack.sh`
- `scripts/prepare_mobile_showcase.sh`

## Что поднимает local stack

- основной backend;
- metric backend;
- локальный APK showcase container.

## Типовой сценарий

```bash
bash scripts/start_mobile_local_stack.sh
```

Остановить:

```bash
bash scripts/stop_mobile_local_stack.sh
```

## Ограничение

Этот сценарий предполагает, что у вас есть локальный checkout репозитория [Registry](https://github.com/AngW1SH/Registry) с рабочими `.env` и compose-конфигами.
