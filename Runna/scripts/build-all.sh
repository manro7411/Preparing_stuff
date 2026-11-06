#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mvn -f "$ROOT/backend/pom.xml" verify
cd "$ROOT/frontend"
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter analyze
flutter test
flutter build web --release --dart-define="API_BASE_URL=${API_BASE_URL:-http://localhost:8080}"