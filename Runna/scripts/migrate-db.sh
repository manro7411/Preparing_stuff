#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ ! -f .env ]]; then
  printf 'Create .env from .env.example first.\n' >&2
  exit 1
fi
docker compose --env-file .env -f deployment/docker/docker-compose.yml up -d --wait postgres
docker compose --env-file .env -f deployment/docker/docker-compose.yml --profile tools run --rm migrate