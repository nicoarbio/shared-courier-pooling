# osrm

Self-hosted [OSRM](https://project-osrm.org/) routing engine. Computes real road-network distance and duration between points in CABA (Buenos Aires city).

- **Map**: Argentina extract ([Geofabrik](https://download.geofabrik.de/south-america/argentina.html)) clipped to a bounding box enclosing CABA.
- **Profile**: car (`car.lua`), so one-way streets are respected.
- **Algorithm**: MLD.

## Usage

```bash
./osrm/prepare.sh            # once: download (~400 MB), clip and preprocess → osrm/data/
docker compose up -d osrm    # API at http://localhost:5000
./osrm/test.sh               # sample queries (nearest + route both ways)
./osrm/test.sh LON,LAT LON,LAT
```

Optional route viewer (visual checks only): `docker compose --profile tools up -d osrm-frontend` → http://localhost:9966

To refresh the map: `rm -rf osrm/data && ./osrm/prepare.sh`.

## API used by the system

Coordinates are **`lon,lat`** (longitude first).

| Service | Example | Returns |
|---|---|---|
| `nearest` | `/nearest/v1/driving/-58.441,-34.620` | closest street to the point |
| `route` | `/route/v1/driving/-58.441,-34.620;-58.438,-34.623?overview=false` | `distance` (m), `duration` (s) |
| `table` | `/table/v1/driving/{p1};{p2};...?annotations=distance,duration` | distance/duration matrix between all points |

Distance A→B may differ from B→A because of one-way streets.
