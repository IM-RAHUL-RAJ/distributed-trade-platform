#!/usr/bin/env bash
# Stop everything and prune ALL Docker caches/images so the VM never bloats.
# After this, "tp-up.sh" brings it back (re-loads from the local tarball).
set -e
REPO="${1:-$HOME/Documents/Projects/OpenCode/CD2026/version1/trade-platform}"

cd "$REPO"
docker compose down
docker system prune -a -f
echo
echo "Pruned. Host disk:"
df -h / | tail -1
echo
echo "Start again anytime with: $REPO/tools/tp-up.sh"