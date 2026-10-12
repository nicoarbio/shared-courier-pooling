-- Case study shops: same block of Yerbal, between F. García Lorca and Félix Lora (Caballito, CABA).
-- Loaded only with the "local" profile.
insert into merchant (id, name, lon, lat) values
    ('00000000-0000-0000-0000-000000000001', 'Panadería San Martín',   -58.44470, -34.62046),
    ('00000000-0000-0000-0000-000000000002', 'Verdulería La Estación', -58.44452, -34.62039),
    ('00000000-0000-0000-0000-000000000003', 'Almacén Don Pedro',      -58.44433, -34.62032),
    ('00000000-0000-0000-0000-000000000004', 'Kiosco Sarmiento',       -58.44415, -34.62025)
on conflict (id) do update set name = excluded.name, lon = excluded.lon, lat = excluded.lat;
