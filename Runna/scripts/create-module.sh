#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODULE="${1:?Usage: create-module.sh module-name}"
if [[ ! "$MODULE" =~ ^[a-z][a-z0-9]*$ ]]; then
  printf 'Module name must be a lowercase Java identifier.\n' >&2
  exit 1
fi
for LAYER in entity dto repository service resource mapper validator; do
  DIRECTORY="$ROOT/backend/src/main/java/io/runcoach/$MODULE/$LAYER"
  mkdir -p "$DIRECTORY"
  if [[ ! -e "$DIRECTORY/package-info.java" ]]; then
    printf 'package io.runcoach.%s.%s;\n' "$MODULE" "$LAYER" > "$DIRECTORY/package-info.java"
  fi
done
printf 'Module scaffold ready: %s\n' "$MODULE"