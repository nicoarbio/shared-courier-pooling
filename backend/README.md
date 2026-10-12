# backend — Mercier

Spring Boot service (Kotlin) that groups neighbourhood shops' orders into shared courier rounds, routes each round over the real street network and splits its cost.

## Engine

`RoundPlanner` runs the pipeline: **group → route → cost → allocate**.

| Step | Class | Rule |
|---|---|---|
| Group | `OrderGrouper` | Complete-linkage, greedy by window start: an order joins a round if every destination stays within `radius-meters` of every shop in the round and all windows start within `window-tolerance`. |
| Route | `NearestNeighborRouter` | Pickups at every shop, then deliveries, then back to the start, each phase by nearest neighbor. Every shop is tried as the start; the shortest route wins. |
| Cost | `CostModel` | `price-per-km` × distance driven. |
| Allocate | `CostAllocator` | Proportional to each shop's delivery stops. |

Two OSRM services over the same street network (see [`../osrm`](../osrm)):
- **proximity** (one-way streets ignored) decides which orders are close enough to share a round;
- **routing** (one-way streets respected) measures what is actually driven, and therefore paid.

Settings live in `src/main/resources/application.yaml` (`mercier.pooling.*`, `mercier.osrm.*`).

## Usage

```bash
./mvnw test                  # unit tests (no OSRM needed)
./mvnw spring-boot:run       # start the service

# Case study run against real OSRM (needs: docker compose up -d osrm osrm-proximity)
./mvnw test -Pscenario       # report → target/scenario-report.md
```
