#!/usr/bin/env bash
# Alien client build script (MC 1.21.11 / JDK 21)
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/tmp/jdk-21.0.13+9}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> Using Java: $(java -version 2>&1 | head -1)"
chmod +x ./gradlew

echo "==> Building..."
./gradlew clean build --no-daemon

echo "==> Artifacts:"
ls -lh build/libs/*.jar
