# frontend

Mobile-first web app (Angular 22 + Angular Material) for the two roles of the system. Spanish UI; no login: the user says who they are.

| Route | Who | What |
|---|---|---|
| `/` | everyone | Choose role |
| `/comercio` | shop | Pick which shop you are (remembered in the browser) |
| `/comercio/:id/nuevo` | shop | New order: search the address as you type, confirm/drag the point on the map, delivery window |
| `/comercio/:id/pedidos` | shop | Your orders: status, round, your share of the cost, estimated delivery (auto-refresh 30 s) |
| `/repartidor` | courier | Rounds ready to leave (auto-refresh 30 s) |
| `/repartidor/rondas/:id` | courier | Ordered stops with times, map, "Open in Google Maps", "Leave" (dispatch) |

## Structure

- `core/`: typed API client (mirrors the backend DTOs), `X-Session-Id` interceptor (one id per tab, for log correlation), error mapping (shows the backend's `X-Request-Id` as an error code), merchant session, polling.
- `i18n/`: every visible text under a key in `es.ts`, used as `{{ 'key' | t }}`. Keys are type-checked: a missing key does not compile. Another language = another file with the same shape.
- `shared/`: Leaflet map over OpenStreetMap tiles (HTML markers, optional draggable point), error message.
- `merchant/`, `courier/`: screens. The new-order behaviour lives in `OrderDraft` (a per-screen service), so the visual layer can change without touching it.

## Usage

```bash
npm install
npm start      # http://localhost:4200 — /api is proxied to the backend on :8080 (proxy.conf.json), so no CORS is needed
npm test       # unit tests (Vitest)
npm run build  # production build → dist/frontend
```

The backend and its services must be running (see the [repository README](../README.md)).
