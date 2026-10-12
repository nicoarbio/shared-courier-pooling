#!/usr/bin/env bash
# Prepares OSRM routing data for CABA (Buenos Aires city).
#
# Steps:
#   1. Download the Argentina extract from Geofabrik (OpenStreetMap).
#   2. Clip it to a bounding box enclosing CABA (osmium).
#   3. Preprocess it with OSRM using the car profile and the MLD algorithm.
#
# Everything runs inside Docker containers: nothing to install locally.
# Output goes to osrm/data/ and is served by the "osrm" docker-compose service.
set -euo pipefail

cd "$(dirname "$0")"
DATA_DIR="$PWD/data"
mkdir -p "$DATA_DIR"

PBF_URL="https://download.geofabrik.de/south-america/argentina-latest.osm.pbf"
OSRM_IMAGE="ghcr.io/project-osrm/osrm-backend:v5.27.1"
OSMIUM_IMAGE="debian:bookworm-slim"

# Containers write to osrm/data/ as the host user (not root),
# so generated files can be removed without sudo.
HOST_USER="$(id -u):$(id -g)"

# CABA bounding box: lon_min,lat_min,lon_max,lat_max
CABA_BBOX="-58.5320,-34.7060,-58.3340,-34.5260"

# 1. Download (skipped if already present)
if [[ ! -f "$DATA_DIR/argentina.osm.pbf" ]]; then
  echo ">> Downloading Argentina extract..."
  curl -L --fail -o "$DATA_DIR/argentina.osm.pbf" "$PBF_URL"
fi

# 2. Clip to CABA
# This container runs as root because it installs osmium via apt-get;
# it hands ownership of the generated file back to the host user at the end.
echo ">> Clipping to CABA ($CABA_BBOX)..."
docker run --rm -v "$DATA_DIR:/data" "$OSMIUM_IMAGE" sh -c "
  apt-get update -qq && apt-get install -y -qq osmium-tool >/dev/null &&
  osmium extract --overwrite -b '$CABA_BBOX' /data/argentina.osm.pbf -o /data/caba.osm.pbf &&
  chown $HOST_USER /data/caba.osm.pbf
"

# 3. OSRM preprocessing (car profile, MLD algorithm)
echo ">> osrm-extract (car profile)..."
docker run --rm --user "$HOST_USER" -v "$DATA_DIR:/data" "$OSRM_IMAGE" osrm-extract -p /opt/car.lua /data/caba.osm.pbf
echo ">> osrm-partition..."
docker run --rm --user "$HOST_USER" -v "$DATA_DIR:/data" "$OSRM_IMAGE" osrm-partition /data/caba.osrm
echo ">> osrm-customize..."
docker run --rm --user "$HOST_USER" -v "$DATA_DIR:/data" "$OSRM_IMAGE" osrm-customize /data/caba.osrm

echo ">> Done. Start with: docker compose up -d osrm"
