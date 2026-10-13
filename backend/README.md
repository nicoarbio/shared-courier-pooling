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

## Persistence

PostgreSQL through JPA/Hibernate; the schema is owned by Flyway (`db/migration`), Hibernate only validates it. `PlanningService` re-plans every order that has not left yet each time a new order arrives, in a single transaction.

## API

Swagger UI at `/swagger-ui.html` (OpenAPI at `/v3/api-docs`). Errors are RFC 9457 problem details.

| Who | Method | Path | What |
|---|---|---|---|
| Merchant | `GET` | `/api/merchants` | List merchants (no login: the UI picks one) |
| Merchant | `POST` | `/api/orders` | Submit an order (coordinates + address text); re-plans every order that has not left yet |
| Merchant | `GET` | `/api/geocode?q=&merchantId=` | Address candidates within the merchant's delivery area (via [Nominatim](../nominatim)), to confirm on a map |
| Merchant | `GET` | `/api/merchants/{id}/orders` | The merchant's orders: round, share of the round's cost, estimated delivery |
| Courier | `GET` | `/api/rounds?status=PLANNED` | Rounds ready to leave |
| Courier | `GET` | `/api/rounds/{id}` | Ordered stops with estimated times, cost split and a Google Maps link |
| Courier | `POST` | `/api/rounds/{id}/dispatch` | The courier leaves: the round is frozen and its times use the actual departure |

Estimated times: departure (earliest delivery window, or now) + driving per OSRM + `stop-service-time` at each pickup and delivery.

## Logging

SLF4J/Logback. Every line carries correlation ids `[requestId sessionId merchantId]`:
- `requestId`: from the `X-Request-Id` header or generated; returned in the response (show it to users when something fails).
- `sessionId`: from the `X-Session-Id` header the UI sends (one per browser tab), to follow a merchant's flow.
- `merchantId`: set by the endpoints that act on behalf of a merchant.

INFO logs business events (order submitted, re-plan summary, dispatch, address search counts) and one line per request; DEBUG adds engine detail and OSRM/Nominatim timings (e.g. `logging.level.com.nicoarbio.mercier=DEBUG`). Customer addresses are personal data and only appear at DEBUG. The `cloud` profile switches to JSON logs (ECS), with the ids as fields.

## Usage

```bash
./mvnw test                  # unit + persistence tests (needs Docker: PostgreSQL via Testcontainers; no OSRM)

# Run the service (needs: docker compose up -d postgres osrm-routing osrm-proximity nominatim)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # "local" also loads the case study shops

# Case study run against real OSRM (needs: docker compose up -d osrm-routing osrm-proximity)
./mvnw test -Pscenario       # report → target/scenario-report.md
```
