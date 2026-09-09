#!/usr/bin/env bash
# VM raw-disk watchdog: if the Docker VM's raw file exceeds the threshold,
# recycle the stack (compose down -> prune -f -> compose up) so the disk never
# hits its cap. Keeps images and named volumes (h2data/kafka-data/analytics-output) intact.
#
# Usage:
#   tp-cycle.sh                # check; recycle only if above threshold
#   tp-cycle.sh --check        # report only, never recycle
#   tp-cycle.sh --force        # recycle now regardless of threshold
set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VMS_DIR="$HOME/Library/Containers/com.docker.docker/Data/vms"
CAP_MB="${TP_VM_CAP_MB:-61440}"
THRESHOLD_MB="${TP_VM_THRESHOLD_MB:-45000}"
MIN_RECYCLE_AGE="$((5 * 60))"
LAST_AT="$REPO/tools/.tp-last-recycle"

MODE="${1:-auto}"
PRUNE="${TP_KEEP_IMAGES:-1}"

COLOUR_OFF='\033[0m'; COLOUR_OK='\033[32m'; COLOUR_WARN='\033[33m'; COLOUR_ERR='\033[31m'
say() { local c="${2:-$COLOUR_OFF}"; printf "%s%s%s\n" "$c" "$1" "$COLOUR_OFF"; }

now_bytes() { du -sk "$VMS_DIR" 2>/dev/null | awk '{print $1 * 1024}'; }

ensure_docker() {
  local tries
  for tries in $(seq 1 15); do
    docker info >/dev/null 2>&1 && return 0
    if [ "$tries" -eq 1 ]; then
      say "[tp-cycle] Docker daemon down — relaunching Docker Desktop" "$COLOUR_WARN"
      pkill -9 -f "Docker.app" >/dev/null 2>&1 || true
      sleep 2
      open -a Docker >/dev/null 2>&1 || open /Applications/Docker.app >/dev/null 2>&1 || true
    fi
    sleep 10
  done
  say "[tp-cycle] Docker daemon still down after relaunch" "$COLOUR_ERR"
  return 1
}

recycle() {
  say "[tp-cycle] VM raw above safe level -> recycling stack (down -> prune -> up)"
  if docker compose -f "$REPO/docker-compose.yml" down; then
    if [ "$PRUNE" = "1" ]; then
      docker system prune -f || true   # containers/caches kept out; images + volumes stay
    fi
    for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 3; done
    docker compose -f "$REPO/docker-compose.yml" up -d
  else
    say "[tp-cycle] FAILED to recycle (compose down errored)" "$COLOUR_ERR"
    return 1
  fi
}

ensure_docker || exit 1

SIZE="$(now_bytes)"
SIZE_MB=$((SIZE / 1024 / 1024))
CAP_GB=$((CAP_MB / 1024)); THRES_GB=$((THRESHOLD_MB / 1024))
gb() { awk -v m="$1" 'BEGIN{printf "%.1f", m/1024}'; }
SIZE_GB="$(gb "$SIZE_MB")"

if [ "$MODE" = "--check" ]; then
  printf "%s GB / %s GB\n" "$SIZE_GB" "$CAP_GB"
  exit 0
fi

if [ "$SIZE_MB" -lt "$THRESHOLD_MB" ]; then
  printf "%sOK: VM raw %sGB below recycle threshold %sGB%s\n" "$COLOUR_OK" "$SIZE_GB" "$THRES_GB" "$COLOUR_OFF"
  exit 0
fi

NOW="$(date +%s)"
LAST="$(cat "$LAST_AT" 2>/dev/null || echo 0)"
if [ "$MODE" != "--force" ] && [ $((NOW - LAST)) -lt "$MIN_RECYCLE_AGE" ]; then
  say "Skipping: recycled $(( (NOW - LAST) / 60 )) min ago (minimum 5 min)" "$COLOUR_WARN"
  exit 0
fi

say "VM raw $SIZE_GB GB >= threshold $THRES_GB GB — recycling" "$COLOUR_WARN"
recycle
date +%s > "$LAST_AT"
say "[tp-cycle] done (leave this stack running; image NOT removed, data kept)" "$COLOUR_OK"