#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT_DIR/scripts/prepare_mobile_showcase.sh"

docker compose -f "$ROOT_DIR/Registry/docker-compose.yaml" up -d --build
docker compose -f "$ROOT_DIR/Registry/metric/docker-compose.server.yaml" up -d --build

docker build -f "$ROOT_DIR/docker/app-showcase/Dockerfile" -t itclinicapp-showcase:local "$ROOT_DIR"

docker rm -f itclinicapp-showcase-local >/dev/null 2>&1 || true
docker run -d --name itclinicapp-showcase-local -p 8080:80 itclinicapp-showcase:local

echo "APK showcase: http://localhost:8080"
echo "Registry backend: http://localhost:8000"
echo "Auth backend: http://localhost:8001"
echo "Metric backend: http://localhost:4173"
