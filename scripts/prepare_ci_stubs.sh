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

escape_kotlin_string() {
  local value="$1"
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  value=${value//\$/\\$}
  printf '%s' "$value"
}

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
    const val AUTH_PRODUCTION_BASE_URL = "https://citec.spb.ru/auth"
    const val LOCAL_PORT = 8000
    const val AUTH_LOCAL_PORT = 3000
    const val LOCAL_HOST_IP = ""
    const val METRIC_PRODUCTION_BASE_URL = "https://metrics.example.com"
    const val METRIC_LOCAL_PORT = 4173
    const val GITHUB_CLIENT_ID = ""
    const val GITHUB_CLIENT_SECRET = ""
}
EOF

mail_smtp_host="$(escape_kotlin_string "${CI_SMTP_HOST:-smtp.invalid}")"
mail_smtp_port="${CI_SMTP_PORT:-465}"
mail_smtp_username="$(escape_kotlin_string "${CI_SMTP_USERNAME:-}")"
mail_smtp_password="$(escape_kotlin_string "${CI_SMTP_PASSWORD:-}")"
mail_smtp_from_email="$(escape_kotlin_string "${CI_SMTP_FROM_EMAIL:-}")"
mail_smtp_from_name="$(escape_kotlin_string "${CI_SMTP_FROM_NAME:-itClinicApp CI}")"
mail_feedback_to_email="$(escape_kotlin_string "${CI_FEEDBACK_TO_EMAIL:-}")"
mail_feedback_subject="$(escape_kotlin_string "${CI_FEEDBACK_SUBJECT:-CI feedback stub}")"

cat > "$MAIL_CONFIG_FILE" <<EOF
package com.spbu.projecttrack

/**
 * CI-generated SMTP configuration.
 *
 * Values are sourced from CI secrets when provided and fall back to
 * placeholder values otherwise.
 */
object MailConfig {
    const val SMTP_HOST = "$mail_smtp_host"
    const val SMTP_PORT = $mail_smtp_port
    const val SMTP_USERNAME = "$mail_smtp_username"
    const val SMTP_PASSWORD = "$mail_smtp_password"
    const val SMTP_FROM_EMAIL = "$mail_smtp_from_email"
    const val SMTP_FROM_NAME = "$mail_smtp_from_name"
    const val FEEDBACK_TO_EMAIL = "$mail_feedback_to_email"
    const val FEEDBACK_SUBJECT = "$mail_feedback_subject"
}
EOF
