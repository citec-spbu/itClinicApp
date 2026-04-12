#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

docker rm -f itclinicapp-showcase-local >/dev/null 2>&1 || true
docker compose -f "$ROOT_DIR/Registry/metric/docker-compose.server.yaml" down
docker compose -f "$ROOT_DIR/Registry/docker-compose.yaml" down
