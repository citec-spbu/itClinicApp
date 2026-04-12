#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_CONFIG_DIR="$ROOT_DIR/composeApp/src/commonMain/kotlin/com/spbu/projecttrack"
BUILD_CONFIG_FILE="$APP_CONFIG_DIR/BuildConfig.kt"
BUILD_CONFIG_EXAMPLE="$APP_CONFIG_DIR/BuildConfig.example.kt"
MAIL_CONFIG_FILE="$APP_CONFIG_DIR/MailConfig.kt"
MAIL_CONFIG_EXAMPLE="$APP_CONFIG_DIR/MailConfig.example.kt"
APK_SOURCE="$ROOT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
APK_TARGET_DIR="$ROOT_DIR/release-artifacts"
APK_TARGET="$APK_TARGET_DIR/itclinicapp-debug.apk"

prepare_config_file() {
  local target_file="$1"
  local example_file="$2"
  local source_name="$3"
  local target_name="$4"

  if [[ -f "$target_file" ]]; then
    return
  fi

  cp "$example_file" "$target_file"
  perl -0pi -e "s/object ${source_name}/object ${target_name}/" "$target_file"
}

prepare_config_file "$BUILD_CONFIG_FILE" "$BUILD_CONFIG_EXAMPLE" "BuildConfigExample" "BuildConfig"
prepare_config_file "$MAIL_CONFIG_FILE" "$MAIL_CONFIG_EXAMPLE" "MailConfigExample" "MailConfig"

"$ROOT_DIR/gradlew" :composeApp:assembleDebug

mkdir -p "$APK_TARGET_DIR"
cp "$APK_SOURCE" "$APK_TARGET"

echo "Prepared APK artifact at: $APK_TARGET"
