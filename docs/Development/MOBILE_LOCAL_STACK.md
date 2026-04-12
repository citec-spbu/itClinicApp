# Mobile Local Docker Stack

## Scope

This setup is for local mobile development.

It reuses the existing backend compose files from `Registry/` and adds the APK showcase container as a separate local container.

It starts:

- the APK delivery container on `http://localhost:8080`
- the main Registry backend on `http://localhost:8000`
- the auth service on `http://localhost:8001`
- the metric backend on `http://localhost:4173`

It does not replace the GitHub CI/CD workflow.
It is only a local Docker setup.

## Files

- helper script: `scripts/prepare_mobile_showcase.sh`
- start script: `scripts/start_mobile_local_stack.sh`
- stop script: `scripts/stop_mobile_local_stack.sh`
- main backend stack: `Registry/docker-compose.yaml`
- metric backend stack: `Registry/metric/docker-compose.server.yaml`
- showcase image: `docker/app-showcase/Dockerfile`
- CI/CD docs: `docs/Development/GITHUB_CICD.md`

## Before first run

Make sure local backend environment files already exist:

- `Registry/.env`
- `Registry/server/.env`
- `Registry/auth/.env`
- `Registry/strapi/.env`
- `Registry/metric/.env`
- `Registry/metric/server/.env`
- `Registry/metric/core/.env`

The stack uses your local backend configuration from these files.

## Build the APK artifact

The showcase container expects the debug APK to be available in `release-artifacts/`.

Run:

```bash
bash scripts/prepare_mobile_showcase.sh
```

## Start the full local stack

Run:

```bash
bash scripts/start_mobile_local_stack.sh
```

## Main endpoints

- APK landing page: `http://localhost:8080`
- main backend API: `http://localhost:8000`
- auth API: `http://localhost:8001`
- metric API: `http://localhost:4173`
- Strapi: `http://localhost:7001`
- Meilisearch: `http://localhost:7700`

## How the mobile app connects

When `USE_LOCAL_API = true`, the mobile app resolves local endpoints through:

- `LOCAL_PORT` for the main backend
- `METRIC_LOCAL_PORT` for the metric backend

With the current stack, the expected ports are:

- main backend: `8000`
- metric backend: `4173`

## Stop the stack

Run:

```bash
bash scripts/stop_mobile_local_stack.sh
```

To also remove named volumes:

```bash
docker compose \
  -f Registry/docker-compose.yaml \
  down -v

docker compose \
  -f Registry/metric/docker-compose.server.yaml \
  down -v
```
