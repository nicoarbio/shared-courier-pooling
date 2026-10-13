-- Dispatch: a round leaves with its courier and is no longer re-planned.
alter table delivery_order drop constraint delivery_order_status_check;
alter table delivery_order add constraint delivery_order_status_check check (status in ('PENDING', 'PLANNED', 'DISPATCHED'));

alter table round add column status varchar(16) not null default 'PLANNED' check (status in ('PLANNED', 'DISPATCHED'));
create index round_status_idx on round (status);

-- Schedule: estimated arrival at a stop = departure_at + arrival_offset_s.
-- departure_at is estimated when planning and replaced by the actual departure on dispatch (city wall-clock time).
alter table round add column departure_at timestamp;
update round set departure_at = planned_at at time zone 'America/Argentina/Buenos_Aires';
alter table round alter column departure_at set not null;

-- Service time per pickup/delivery the round was planned with.
alter table round add column service_time_s double precision not null default 0;

-- Per stop: drive from the previous stop (as measured by OSRM) and cumulative time since departure.
alter table round_stop add column leg_distance_m   double precision not null default 0;
alter table round_stop add column leg_duration_s   double precision not null default 0;
alter table round_stop add column arrival_offset_s double precision not null default 0;
