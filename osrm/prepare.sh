#!/usr/bin/env bash
# Prepares OSRM data for CABA (Buenos Aires city).
#
# Steps:
#   1. Download the Argentina extract from Geofabrik (OpenStreetMap).
#   2. Clip it to a bounding box enclosing CABA (osmium).
#   3. Preprocess it with OSRM (MLD algorithm) into two datasets:
#      - routing:   stock car profile; respects one-way streets. Used for routes and costs.
#      - proximity: car profile with one-way handling disabled. Used to group orders by
#                   street-network closeness, independent of traffic direction.
#
# Everything runs inside Docker containers: nothing to install locally.
# Output goes to osrm/data/ and is served by the docker-compose services.
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

# 3. OSRM preprocessing, one dataset per profile.
# Each dataset dir gets its own copy of the stock profiles (car.lua + lib/), so the
# proximity profile can be derived from the image's car.lua and stay in sync with it.
osrm() { docker run --rm --user "$HOST_USER" -v "$DATA_DIR:/data" "$OSRM_IMAGE" "$@"; }

build() { # $1 = dataset name, $2 = profile file inside the dataset dir
  local dir="/data/$1"
  echo ">> [$1] osrm-extract ($2)..."
  osrm osrm-extract -p "$dir/$2" "$dir/caba.osm.pbf"
  echo ">> [$1] osrm-partition..."
  osrm osrm-partition "$dir/caba.osrm"
  echo ">> [$1] osrm-customize..."
  osrm osrm-customize "$dir/caba.osrm"
}

for dataset in routing proximity; do
  rm -rf "${DATA_DIR:?}/$dataset" && mkdir -p "$DATA_DIR/$dataset"
  cp "$DATA_DIR/caba.osm.pbf" "$DATA_DIR/$dataset/"
  osrm sh -c "cp -r /opt/car.lua /opt/lib /data/$dataset/"
done
sed -i 's/oneway_handling *= *true/oneway_handling = false/' "$DATA_DIR/proximity/car.lua"
grep -q 'oneway_handling = false' "$DATA_DIR/proximity/car.lua" || { echo "!! could not disable oneway_handling"; exit 1; }

build routing car.lua
build proximity car.lua

echo ">> Done. Start with: docker compose up -d osrm osrm-proximity"
