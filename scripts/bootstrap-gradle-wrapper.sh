#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_VERSION="${GRADLE_VERSION:-8.11.1}"

cd "$ROOT_DIR"

if command -v gradle >/dev/null 2>&1; then
  gradle wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required when system gradle is unavailable" >&2
  exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "unzip is required when system gradle is unavailable" >&2
  exit 1
fi

BOOTSTRAP_DIR="$ROOT_DIR/build/gradle-bootstrap"
ZIP_PATH="$BOOTSTRAP_DIR/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_BIN="$BOOTSTRAP_DIR/gradle-${GRADLE_VERSION}/bin/gradle"

mkdir -p "$BOOTSTRAP_DIR"

if [[ ! -x "$GRADLE_BIN" ]]; then
  curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP_PATH"
  unzip -q "$ZIP_PATH" -d "$BOOTSTRAP_DIR"
fi

"$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin

