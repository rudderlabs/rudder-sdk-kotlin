#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"

write_local_properties() {
  local sdk_path="$1"
  echo "sdk.dir=$sdk_path" > "$LOCAL_PROPERTIES"
  return 0
}

export_android_home() {
  local sdk_path="$1"
  echo "ANDROID_HOME=$sdk_path" >> "$GITHUB_ENV"
  echo "ANDROID_SDK_ROOT=$sdk_path" >> "$GITHUB_ENV"
  echo "$sdk_path/platform-tools" >> "$GITHUB_PATH"
  echo "$sdk_path/cmdline-tools/latest/bin" >> "$GITHUB_PATH"
  return 0
}

# Check well-known locations
for candidate in \
  "${ANDROID_HOME:-}" \
  "${ANDROID_SDK_ROOT:-}" \
  "/usr/local/lib/android/sdk" \
  "$HOME/android-sdk" \
  "$HOME/Android/Sdk" \
  "/opt/android-sdk"
do
  if [[ -n "$candidate" ]] && [[ -d "$candidate" ]]; then
    echo "Android SDK found at: $candidate"
    export_android_home "$candidate"
    write_local_properties "$candidate"
    exit 0
  fi
done

# Install Android SDK
SDK_DIR="$HOME/android-sdk"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
INSTALL_DIR="$SDK_DIR/cmdline-tools/latest"

echo "Android SDK not found. Installing to $SDK_DIR..."

mkdir -p "$INSTALL_DIR"
curl -fsSL "$CMDLINE_TOOLS_URL" -o "/tmp/$CMDLINE_TOOLS_ZIP"
unzip -q "/tmp/$CMDLINE_TOOLS_ZIP" -d "/tmp/cmdline-tools-extract"
mv /tmp/cmdline-tools-extract/cmdline-tools/* "$INSTALL_DIR/"
rm -rf "/tmp/$CMDLINE_TOOLS_ZIP" "/tmp/cmdline-tools-extract"

export ANDROID_HOME="$SDK_DIR"
export PATH="$INSTALL_DIR/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --licenses > /dev/null 2>&1 || true

sdkmanager \
  "platform-tools" \
  "build-tools;35.0.0" \
  "platforms;android-35"

export_android_home "$SDK_DIR"
write_local_properties "$SDK_DIR"

echo "Android SDK installed at: $SDK_DIR"
