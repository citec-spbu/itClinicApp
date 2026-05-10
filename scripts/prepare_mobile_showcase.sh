#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_DIR="$ROOT_DIR/composeApp/src/commonMain/kotlin/com/spbu/projecttrack"
BUILD_CONFIG_FILE="$CONFIG_DIR/BuildConfig.kt"
MAIL_CONFIG_FILE="$CONFIG_DIR/MailConfig.kt"
DELIVERY_BUILD_TYPE="${ANDROID_DELIVERY_BUILD_TYPE:-debug}"
APK_SOURCE="$ROOT_DIR/composeApp/build/outputs/apk/${DELIVERY_BUILD_TYPE}/composeApp-${DELIVERY_BUILD_TYPE}.apk"
APK_TARGET_DIR="$ROOT_DIR/release-artifacts"
APK_TARGET_NAME="${UPDATE_ASSET_NAME:-itclinicapp-${DELIVERY_BUILD_TYPE}.apk}"
APK_TARGET="$APK_TARGET_DIR/$APK_TARGET_NAME"
BACKUP_DIR="$(mktemp -d)"
HAD_BUILD_CONFIG=0
HAD_MAIL_CONFIG=0

cleanup() {
  if [[ "$HAD_BUILD_CONFIG" -eq 1 ]]; then
    cp "$BACKUP_DIR/BuildConfig.kt" "$BUILD_CONFIG_FILE"
  else
    rm -f "$BUILD_CONFIG_FILE"
  fi

  if [[ "$HAD_MAIL_CONFIG" -eq 1 ]]; then
    cp "$BACKUP_DIR/MailConfig.kt" "$MAIL_CONFIG_FILE"
  else
    rm -f "$MAIL_CONFIG_FILE"
  fi

  rm -rf "$BACKUP_DIR"
}

if [[ -f "$BUILD_CONFIG_FILE" ]]; then
  cp "$BUILD_CONFIG_FILE" "$BACKUP_DIR/BuildConfig.kt"
  HAD_BUILD_CONFIG=1
fi

if [[ -f "$MAIL_CONFIG_FILE" ]]; then
  cp "$MAIL_CONFIG_FILE" "$BACKUP_DIR/MailConfig.kt"
  HAD_MAIL_CONFIG=1
fi

trap cleanup EXIT

FORCE_CI_STUBS=1 bash "$ROOT_DIR/scripts/prepare_ci_stubs.sh"

# Build from a clean composeApp state so stale dex archives do not leak
# duplicate classes like "* 2.dex" into the APK packaging step.
if [[ "$DELIVERY_BUILD_TYPE" == "release" ]]; then
  "$ROOT_DIR/gradlew" :composeApp:clean :composeApp:assembleRelease
else
  "$ROOT_DIR/gradlew" :composeApp:clean :composeApp:assembleDebug
fi

mkdir -p "$APK_TARGET_DIR"
rm -f "$APK_TARGET"
cp "$APK_SOURCE" "$APK_TARGET"

echo "Prepared APK artifact at: $APK_TARGET"
