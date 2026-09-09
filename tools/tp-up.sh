#!/usr/bin/env bash
# Start the full trade-platform stack using cached prebuilt images.
# If the images were pruned, they are re-loaded from the local tarball.
set -e
REPO="${1:-$HOME/Documents/Projects/OpenCode/CD2026/version1/trade-platform}"
TAR="${HOME}/Projects/tp-images.tar.gz"

cd "$REPO"

# Load images only if they are missing (docker load ~30s otherwise).
if ! docker image inspect trade-platform-service-1 >/dev/null 2>&1; then
  if [ ! -f "$TAR" ]; then
    echo "Downloading prebuilt images (0.7 GB)..."
    curl -fsSL -o "$TAR" \
      https://github.com/IM-RAHUL-RAJ/distributed-trade-platform/releases/latest/download/trade-platform-images.tar.gz
  fi
  echo "Loading images..."
  docker load -i "$TAR"
fi

docker compose up -d
echo "Waiting for services to become healthy..."
sleep 45
docker compose ps
echo
echo "UI:    http://localhost:4200"
echo "BFF:   http://localhost:3000/api/v1"
echo "Logs:  docker compose -f $REPO/docker-compose.yml logs -f service-2"