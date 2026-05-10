#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHANGELOG_FILE_REL="${1:-release-notes/android-update-notes.txt}"
CHANGELOG_FILE="$ROOT_DIR/$CHANGELOG_FILE_REL"

if [[ ! -f "$CHANGELOG_FILE" ]]; then
  echo "[]"
  exit 0
fi

CURRENT_CONTENT="$(cat "$CHANGELOG_FILE")"
PREVIOUS_CONTENT=""

if git -C "$ROOT_DIR" rev-parse --verify HEAD^ >/dev/null 2>&1; then
  if git -C "$ROOT_DIR" cat-file -e "HEAD^:$CHANGELOG_FILE_REL" 2>/dev/null; then
    PREVIOUS_CONTENT="$(git -C "$ROOT_DIR" show "HEAD^:$CHANGELOG_FILE_REL")"
  fi
fi

if [[ "$CURRENT_CONTENT" == "$PREVIOUS_CONTENT" ]]; then
  echo "[]"
  exit 0
fi

python3 - "$CHANGELOG_FILE" <<'PY'
import json
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
items = []

for raw_line in path.read_text(encoding="utf-8").splitlines():
    line = raw_line.strip()
    if not line or line.startswith("#"):
        continue
    line = re.sub(r"^[-*]\s*", "", line).strip()
    if line:
        items.append(line)

print(json.dumps(items, ensure_ascii=False))
PY
