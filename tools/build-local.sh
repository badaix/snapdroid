#!/usr/bin/env bash
set -euo pipefail
# Run from any directory; provide a normal JDK, Android SDK and a native client.
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 17 or newer}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
: "${SNAPCAST_NATIVE_DIR:?Directory containing ABI/libsnapclient.so from v0.29.0.2}"
repo_dir=$(cd -- "$(dirname -- "$0")/.." && pwd)
bash "$repo_dir/gradlew" -p "$repo_dir" \
  "-PprebuiltClientDir=$SNAPCAST_NATIVE_DIR" "$@"
