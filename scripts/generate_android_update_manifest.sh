#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/release-artifacts"
OUTPUT_FILE="$OUTPUT_DIR/android-update.json"
ASSET_NAME="${UPDATE_ASSET_NAME:-itclinicapp-debug.apk}"

: "${UPDATE_REPOSITORY_OWNER:?UPDATE_REPOSITORY_OWNER is required}"
: "${UPDATE_REPOSITORY_NAME:?UPDATE_REPOSITORY_NAME is required}"
: "${UPDATE_RELEASE_TAG:?UPDATE_RELEASE_TAG is required}"
: "${UPDATE_VERSION_CODE:?UPDATE_VERSION_CODE is required}"
: "${UPDATE_VERSION_NAME:?UPDATE_VERSION_NAME is required}"

mkdir -p "$OUTPUT_DIR"

cat > "$OUTPUT_FILE" <<EOF
{
  "versionCode": ${UPDATE_VERSION_CODE},
  "versionName": "${UPDATE_VERSION_NAME}",
  "releaseTag": "${UPDATE_RELEASE_TAG}",
  "apkUrl": "https://github.com/${UPDATE_REPOSITORY_OWNER}/${UPDATE_REPOSITORY_NAME}/releases/download/${UPDATE_RELEASE_TAG}/${ASSET_NAME}",
  "releasePageUrl": "https://github.com/${UPDATE_REPOSITORY_OWNER}/${UPDATE_REPOSITORY_NAME}/releases/tag/${UPDATE_RELEASE_TAG}",
  "commitSha": "${UPDATE_COMMIT_SHA:-}",
  "generatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF

echo "Prepared Android update manifest at: $OUTPUT_FILE"
