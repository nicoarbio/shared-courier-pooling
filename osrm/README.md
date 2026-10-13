# osrm

Self-hosted [OSRM](https://project-osrm.org/) routing engine. Computes real road-network distance and duration between points in CABA (Buenos Aires city).

- **Map**: Argentina extract ([Geofabrik](https://download.geofabrik.de/south-america/argentina.html)) clipped to a bounding box enclosing CABA.
- **Algorithm**: MLD.
- **Two datasets / services**, same street network:

| Service | Port | Profile | Used for |
|---|---|---|---|
| `osrm-routing` | 5000 | stock car (`car.lua`): respects one-way streets | routes and round cost (what is actually driven) |
| `osrm-proximity` | 5001 | car with `oneway_handling = false` | grouping orders by street-network closeness, regardless of traffic direction |

One-way streets make driving distance very asymmetric at block scale (e.g. 85 m one way, 1,257 m the other), which would split neighbouring orders for no geographic reason. Proximity still follows the real network (railways, missing crossings), unlike straight-line distance.

## Usage

```bash
./osrm/prepare.sh                            # once: download (~400 MB), clip and preprocess → osrm/data/
docker compose up -d osrm-routing osrm-proximity  # APIs at :5000 and :5001
./osrm/test.sh                               # sample queries (nearest + route both ways)
./osrm/test.sh LON,LAT LON,LAT
OSRM_URL=http://localhost:5001 ./osrm/test.sh   # same queries against the proximity service
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
