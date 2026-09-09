#!/usr/bin/env bash
# Run the VM disk watchdog in a loop. Ctrl+C to stop.
# Start:   nohup tools/tp-watch.sh > tools/tp-watch.log 2>&1 &
# Stop:    pkill -f tp-watch
INTERVAL="${TP_WATCH_INTERVAL:-60}"
SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
while true; do
  "$SELF_DIR/tp-cycle.sh"
  sleep "$INTERVAL"
done