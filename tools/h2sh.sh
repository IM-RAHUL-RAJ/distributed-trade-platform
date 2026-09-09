#!/usr/bin/env bash
# Query the embedded H2 database (runs inside the service-1 container).
#
# Usage:
#   tools/h2sh.sh -sql "SELECT COUNT(*) FROM instruments"
#   tools/h2sh.sh -interactive
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

INTERACTIVE=0
ARGS=()
if [ "${1:-}" = "-interactive" ]; then
  INTERACTIVE=1
else
  ARGS=( "$@" )
fi

SHELL_ARGS="-url \"jdbc:h2:tcp://localhost:9095/trade_platform\" -user sa -password \"\""
if (( ${#ARGS[@]} > 0 )); then
  for a in "${ARGS[@]}"; do SHELL_ARGS="$SHELL_ARGS $(printf '%q' "$a")"; done
fi

docker compose exec${INTERACTIVE:+} service-1 sh -c "
  cd /tmp
  [ -f h2.jar ] || wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar
  exec java -cp h2.jar org.h2.tools.Shell $SHELL_ARGS"