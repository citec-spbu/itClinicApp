#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${UPDATE_OUTPUT_DIR:-$ROOT_DIR/release-artifacts}"
OUTPUT_FILE="${UPDATE_OUTPUT_FILE:-$OUTPUT_DIR/android-update.json}"
CHANNEL_MANIFEST_DIR="${UPDATE_CHANNEL_MANIFEST_DIR:-$OUTPUT_DIR/update-manifests}"
APK_FILE_NAME="${UPDATE_ASSET_NAME:-itclinicapp-release.apk}"
APK_FILE_PATH="${UPDATE_APK_PATH:-$OUTPUT_DIR/$APK_FILE_NAME}"
VERSION_CODE="${UPDATE_VERSION_CODE:-${ANDROID_VERSION_CODE:-}}"
VERSION_NAME="${UPDATE_VERSION_NAME:-${ANDROID_VERSION_NAME:-}}"
UPDATE_CHANNEL="${UPDATE_CHANNEL:-beta}"
MIN_SUPPORTED_VERSION_CODE="${UPDATE_MIN_SUPPORTED_VERSION_CODE:-0}"
FORCE_UPDATE="${UPDATE_FORCE_UPDATE:-false}"
CHANGELOG_JSON="${UPDATE_CHANGELOG_JSON:-[]}"

: "${UPDATE_REPOSITORY_OWNER:?UPDATE_REPOSITORY_OWNER is required}"
: "${UPDATE_REPOSITORY_NAME:?UPDATE_REPOSITORY_NAME is required}"
: "${UPDATE_RELEASE_TAG:?UPDATE_RELEASE_TAG is required}"
: "${VERSION_CODE:?UPDATE_VERSION_CODE or ANDROID_VERSION_CODE is required}"
: "${VERSION_NAME:?UPDATE_VERSION_NAME or ANDROID_VERSION_NAME is required}"

if [[ ! -f "$APK_FILE_PATH" ]]; then
  echo "APK not found at $APK_FILE_PATH" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR" "$CHANNEL_MANIFEST_DIR"

SHA256="$(shasum -a 256 "$APK_FILE_PATH" | awk '{print $1}')"
SIZE_BYTES="$(wc -c < "$APK_FILE_PATH" | tr -d '[:space:]')"
RELEASE_PAGE_URL="https://github.com/${UPDATE_REPOSITORY_OWNER}/${UPDATE_REPOSITORY_NAME}/releases/tag/${UPDATE_RELEASE_TAG}"
APK_URL="https://github.com/${UPDATE_REPOSITORY_OWNER}/${UPDATE_REPOSITORY_NAME}/releases/download/${UPDATE_RELEASE_TAG}/${APK_FILE_NAME}"
GENERATED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

cat > "$OUTPUT_FILE" <<EOF
{
  "channel": "${UPDATE_CHANNEL}",
  "versionCode": ${VERSION_CODE},
  "versionName": "${VERSION_NAME}",
  "minSupportedVersionCode": ${MIN_SUPPORTED_VERSION_CODE},
  "forceUpdate": ${FORCE_UPDATE},
  "releaseTag": "${UPDATE_RELEASE_TAG}",
  "apkUrl": "${APK_URL}",
  "sha256": "${SHA256}",
  "sizeBytes": ${SIZE_BYTES},
  "releasePageUrl": "${RELEASE_PAGE_URL}",
  "changelog": ${CHANGELOG_JSON},
  "commitSha": "${UPDATE_COMMIT_SHA:-}",
  "generatedAt": "${GENERATED_AT}"
}
EOF

cp "$OUTPUT_FILE" "$CHANNEL_MANIFEST_DIR/${UPDATE_CHANNEL}.json"

echo "Prepared Android update manifest at: $OUTPUT_FILE"
echo "Prepared channel manifest at: $CHANNEL_MANIFEST_DIR/${UPDATE_CHANNEL}.json"
