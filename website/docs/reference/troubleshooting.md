---
title: Troubleshooting
---

# Troubleshooting

## Локальный API не отвечает

Проверьте:

- `BuildConfig.USE_LOCAL_API`
- правильность `LOCAL_HOST_IP`, если нужен ручной override;
- доступность backend stack;
- значение base URL в `NetworkDebugScreen`.

## GitHub auth не стартует

Проверьте:

- `AUTH_PRODUCTION_BASE_URL` или локальный auth host;
- `GITHUB_CLIENT_ID` и `GITHUB_CLIENT_SECRET`;
- redirect URL scheme `itclinicapp://auth/callback`;
- доступность auth backend.

## Statistics screen показывает странные значения

Проверьте:

- выбранный repository;
- выбранный диапазон дат;
- rapid PR threshold;
- является ли это client-side workaround, а не server-side bug.

## CI ломается из-за config-файлов

Проверьте:

- `scripts/prepare_ci_stubs.sh`
- `scripts/prepare_mobile_showcase.sh`
- `.github/workflows/mobile-app-ci-cd.yml`

## Docs-site не поднимается

Проверьте:

- `cd website`
- `npm install`
- `npm run start`
