#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_DIR="$ROOT_DIR/composeApp/src/commonMain/kotlin/com/spbu/projecttrack"
BUILD_CONFIG_FILE="$CONFIG_DIR/BuildConfig.kt"
MAIL_CONFIG_FILE="$CONFIG_DIR/MailConfig.kt"

mkdir -p "$CONFIG_DIR"

if [[ "${CI:-}" != "true" && "${FORCE_CI_STUBS:-0}" != "1" ]]; then
  if [[ -f "$BUILD_CONFIG_FILE" || -f "$MAIL_CONFIG_FILE" ]]; then
    echo "Refusing to overwrite local config files outside CI."
    echo "Use FORCE_CI_STUBS=1 only from scripted ephemeral flows."
    exit 1
  fi
fi

cat > "$BUILD_CONFIG_FILE" <<'EOF'
package com.spbu.projecttrack

/**
 * CI-safe fallback configuration generated in ephemeral environments.
 *
 * This file is intentionally not committed and should never contain real
 * developer or production secrets.
 */
object BuildConfig {
    const val TEST_TOKEN = ""
    const val USE_LOCAL_API = false
    const val PRODUCTION_BASE_URL = "https://citec.spb.ru/api"
    const val LOCAL_PORT = 8000
    const val LOCAL_HOST_IP = ""
    const val METRIC_PRODUCTION_BASE_URL = "https://metrics.example.com"
    const val METRIC_LOCAL_PORT = 4173
    const val GITHUB_CLIENT_ID = ""
    const val GITHUB_CLIENT_SECRET = ""
}
EOF

cat > "$MAIL_CONFIG_FILE" <<'EOF'
package com.spbu.projecttrack

/**
 * CI-safe fallback SMTP configuration.
 *
 * Runtime feedback delivery is not exercised in CI, so these placeholder
 * values only exist to satisfy compilation.
 */
object MailConfig {
    const val SMTP_HOST = "smtp.invalid"
    const val SMTP_PORT = 465
    const val SMTP_USERNAME = ""
    const val SMTP_PASSWORD = ""
    const val SMTP_FROM_EMAIL = ""
    const val SMTP_FROM_NAME = "itClinicApp CI"
    const val FEEDBACK_TO_EMAIL = ""
    const val FEEDBACK_SUBJECT = "CI feedback stub"
}
EOF
