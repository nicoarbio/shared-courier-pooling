#!/usr/bin/env bash
# Manual check of the local OSRM service (started with: docker compose up -d osrm).
#
# Exercises the two OSRM services the system relies on:
#   - nearest: snaps a point to the closest street in the map.
#   - route:   real road-network distance (m) and duration (s) between two points.
#
# OSRM coordinate format: lon,lat (longitude first!).
#
# Usage:
#   ./osrm/test.sh                       # sample points near Primera Junta
#   ./osrm/test.sh LON1,LAT1 LON2,LAT2   # custom points
set -euo pipefail

OSRM_URL="${OSRM_URL:-http://localhost:5000}"
A="${1:--58.4410,-34.6200}"
B="${2:--58.4380,-34.6230}"

echo "== nearest: closest street to A ($A)"
curl -s "$OSRM_URL/nearest/v1/driving/$A?number=1" | python3 -c '
import sys, json
for w in json.load(sys.stdin)["waypoints"]:
    print("  %s - snapped to %s, %.1f m away" % (w["name"], w["location"], w["distance"]))
'

# Queried both ways: because of one-way streets,
# network distance A->B can differ from B->A.
for leg in "$A;$B" "$B;$A"; do
  echo "== route: ${leg/;/ -> }"
  curl -s "$OSRM_URL/route/v1/driving/$leg?overview=false" | python3 -c '
import sys, json
r = json.load(sys.stdin)["routes"][0]
print("  distance: %.0f m - duration: %.0f s" % (r["distance"], r["duration"]))
'
done
