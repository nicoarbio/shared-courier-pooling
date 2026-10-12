-- Coordinates are WGS84 longitude/latitude. All distances are computed by OSRM, never in the database.
-- Delivery windows are wall-clock times of the city (timestamp without time zone);
-- audit instants (planned_at, created_at) are absolute (timestamptz).

create table merchant (
    id   uuid primary key,
    name text             not null,
    lon  double precision not null,
    lat  double precision not null
);

create table round (
    id           uuid primary key,
    planned_at   timestamptz      not null,
    distance_m   double precision not null,
    duration_s   double precision not null,
    cost         numeric(12, 2)   not null,
    -- Parameters the round was planned with, so it can be explained after the configuration changes.
    price_per_km numeric(12, 2)   not null,
    radius_m     double precision not null
);

create table delivery_order (
    id           uuid primary key,
    merchant_id  uuid             not null references merchant (id),
    dest_lon     double precision not null,
    dest_lat     double precision not null,
    address      text,
    window_start timestamp        not null,
    window_end   timestamp        not null,
    status       varchar(16)      not null check (status in ('PENDING', 'PLANNED')),
    round_id     uuid references round (id),
    created_at   timestamptz      not null,
    check (window_end >= window_start)
);

create index delivery_order_status_idx on delivery_order (status);

-- Ordered route of a round.
create table round_stop (
    round_id    uuid             not null references round (id) on delete cascade,
    seq         integer          not null,
    kind        varchar(16)      not null check (kind in ('PICKUP', 'DELIVERY', 'RETURN')),
    merchant_id uuid             not null references merchant (id),
    order_id    uuid references delivery_order (id),
    lon         double precision not null,
    lat         double precision not null,
    primary key (round_id, seq)
);

-- What each merchant pays for a round.
create table round_share (
    round_id    uuid           not null references round (id) on delete cascade,
    merchant_id uuid           not null references merchant (id),
    amount      numeric(12, 2) not null,
    primary key (round_id, merchant_id)
);
