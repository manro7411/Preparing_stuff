#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ ! -f .env ]]; then
  printf 'Create .env from .env.example and configure local credentials first.\n' >&2
  exit 1
fi
set -a
source "$ROOT/.env"
set +a
docker compose --env-file .env -f deployment/docker/docker-compose.yml up -d --wait postgres
exec mvn -f backend/pom.xml quarkus:dev