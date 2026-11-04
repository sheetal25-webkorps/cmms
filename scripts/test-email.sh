#!/usr/bin/env bash
# Sends a test email using the SMTP settings in .env, to confirm the server can send mail.
# Usage (from the project root, on the server):  ./scripts/test-email.sh you@example.com
set -euo pipefail

TO="${1:?Usage: $0 recipient@example.com}"
ENV_FILE="$(dirname "$0")/../.env"
[ -f "$ENV_FILE" ] || { echo "No .env found at $ENV_FILE"; exit 1; }

get() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2-; }
HOST="$(get SMTP_HOST)"; PORT="$(get SMTP_PORT)"
USER="$(get SMTP_USER)"; PASS="$(get SMTP_PWD)"; FROM="$(get SMTP_FROM)"
FROM="${FROM:-$USER}"

if [ "$(get ENABLE_EMAIL_NOTIFICATIONS)" != "true" ]; then
  echo "WARNING: ENABLE_EMAIL_NOTIFICATIONS is not 'true' in .env - the app will refuse email actions."
fi

MSG="$(mktemp)"; trap 'rm -f "$MSG"' EXIT
cat > "$MSG" <<EOF
From: CMMS <$FROM>
To: <$TO>
Subject: CMMS SMTP test
Date: $(date -R)

If you can read this, the CMMS server can send email through $HOST:$PORT.
EOF

echo "Sending test email via $HOST:$PORT as $USER to $TO ..."
if curl --silent --show-error --url "smtp://$HOST:$PORT" --ssl-reqd \
     --user "$USER:$PASS" --mail-from "$FROM" --mail-rcpt "$TO" \
     --upload-file "$MSG" --connect-timeout 15; then
  echo "OK: email sent. Check the inbox (and spam) of $TO."
else
  echo "FAILED. Common causes:"
  echo " - '535 Username and Password not accepted': SMTP_PWD must be a Gmail App Password, not the account password."
  echo " - Timeout / connection refused: the server's provider blocks outbound port $PORT (ask the provider to allow outbound SMTP on port 587)."
  exit 1
fi
