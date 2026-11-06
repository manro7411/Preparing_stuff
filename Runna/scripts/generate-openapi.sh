#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mvn -f "$ROOT/backend/pom.xml" package -DskipTests
printf 'OpenAPI schemas: %s/backend/target/openapi/openapi.yaml and openapi.json\n' "$ROOT"