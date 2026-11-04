#!/usr/bin/env bash
# Runs the API locally against the Postgres "atlas" database on this Mac, using email/JWT settings from .env.
# Usage (from the project root):  ./scripts/run-api-local.sh [--build]   (--build after changing Java code)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MVN="${MVN:-$(ls -d "$HOME"/.m2/wrapper/dists/apache-maven-3.9.5-bin/*/apache-maven-3.9.5/bin/mvn | head -1)}"

# Read with a loop: `source <(...)` silently loads nothing in macOS's bash 3.2.
while IFS= read -r line; do
  export "${line%%=*}=${line#*=}"
done < <(grep -E '^(JWT_SECRET_KEY|ENABLE_EMAIL_NOTIFICATIONS|INVITATION_VIA_EMAIL|MAIL_TYPE|SMTP_[A-Z]+|MAIL_RECIPIENTS|LDAP_[A-Z_]+|ENABLE_SSO)=' "$ROOT/.env")
: "${JWT_SECRET_KEY:?JWT_SECRET_KEY missing from .env}"

set -a
DB_URL=localhost:5432/atlas
DB_USER="${DB_USER:-$USER}"
DB_PWD="${DB_PWD:-}"
PUBLIC_API_URL=http://localhost:8080
PUBLIC_FRONT_URL=http://localhost:3000
STORAGE_TYPE=minio
ENABLE_MAIL_HEALTH_CHECK=false
set +a

cd "$ROOT/api"
# Build only when asked (after code changes) or when there is no build yet.
# Lombok 1.18.38 is needed to compile on Java 22+.
if [ "${1:-}" = "--build" ] || [ ! -f target/cp.txt ]; then
  "$MVN" -q -Dlombok.version=1.18.38 compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi
exec java -cp "target/classes:$(cat target/cp.txt)" com.grash.ApiApplication
