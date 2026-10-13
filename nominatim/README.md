# nominatim

Self-hosted [Nominatim](https://nominatim.org/) geocoder: turns the address a merchant types into coordinates. It runs over the same CABA extract as OSRM (`osrm/data/caba.osm.pbf`, built by `osrm/prepare.sh`), so geocoding and routing share the same map data.

## Usage

```bash
./osrm/prepare.sh                 # once, if osrm/data/caba.osm.pbf does not exist yet
docker compose up -d nominatim    # first start imports the extract (~7 min); API at http://localhost:8088
curl "http://localhost:8088/search?q=Yerbal+1550&format=jsonv2&addressdetails=1"
```

The imported database lives in the `nominatim-data` volume. To re-import after refreshing the map: `docker compose rm -sf nominatim && docker volume rm shared-courier-pooling_nominatim-data`, then start it again.

## How the backend uses it

`GET /api/geocode?q=...&merchantId=...` searches only within ~2 km of the merchant (`viewbox` + `bounded=1`), which avoids same-name streets elsewhere in the city. The UI shows the candidates and the point on a map for the merchant to confirm.

Known limits: matches whole words, not prefixes (no true autocomplete; the UI searches as the user types); street intersections are not supported.
