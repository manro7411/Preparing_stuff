#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mvn -f "$ROOT/backend/pom.xml" clean
cd "$ROOT/frontend"
flutter clean
printf 'Build output removed. Database volumes and source files were preserved.\n'