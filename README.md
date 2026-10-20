# GridWatch

Community-driven electricity outage detection for South Africa. Households report whether
their power is `ON`, `OFF`, `UNSTABLE` or `RESTORED`; GridWatch groups nearby reports in time
and space and decides whether there is a localized outage, and how confident it is.

It answers: *is this just my house, or is the whole area down?*

## Status

| Area | State |
| ---- | ----- |
| Reporting API (create, nearby) | Done |
| Rule-based outage detection and restoration | Done |
| Stale outage auto-expiry | Done |
| Outage API (list, by id, nearby) | Done |
| Validation and JSON error responses | Done |
| Auth, devices, "Is It Just Me?", frontend, IoT, cloud | Planned |

## Quick start

Requires Java 21+, Maven and Docker.

```bash
docker run -d --name gridwatch-db \
  -e POSTGRES_DB=gridwatch -e POSTGRES_USER=gridwatch -e POSTGRES_PASSWORD=gridwatch \
  -p 5432:5432 -v gridwatch-data:/var/lib/postgresql/data postgres:16

mvn spring-boot:run
```

Then post three nearby `OFF` reports and list outages:

```bash
for i in 1 2 3; do
  curl -s -X POST localhost:8080/api/reports -H 'Content-Type: application/json' \
    -d '{"status":"OFF","latitude":-26.2041,"longitude":28.0473}'; echo
done
curl -s localhost:8080/api/outages
```

See [docs/HOW_IT_RUNS.md](docs/HOW_IT_RUNS.md) for the full walkthrough and troubleshooting.

## API

| Endpoint | Description |
| -------- | ----------- |
| `POST /api/reports` | Submit a report (`status`, `latitude`, `longitude`, optional `description`) |
| `GET /api/reports/nearby?lat=&lon=&radius=&minutes=` | Recent reports near a point (radius 1-50000 m, default 2000; minutes 1-1440, default 60) |
| `GET /api/outages` | Active (non-restored) outages |
| `GET /api/outages/{id}` | One outage, `404` if unknown |
| `GET /api/outages/nearby?lat=&lon=&radius=` | Active outages near a point |
| `GET /actuator/health` | Health check |

Planned: `/api/auth/*`, `/api/devices/*`, an "Is It Just Me?" endpoint.

## How detection works (short version)

Three or more `OFF`/`UNSTABLE` reports within 1 km and 30 minutes make a `LIKELY` outage;
five make it `CONFIRMED`. Two `RESTORED`/`ON` reports nearby close it, and outages with no
activity for 3 hours are closed automatically. Details in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Tech stack

Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation,
PostgreSQL 16, Maven, JUnit 5 and Mockito. Planned: PostGIS, React, ESP32/MQTT, cloud hosting.

## Tests

```bash
mvn test | grep -E "Tests run:|BUILD"
```

## Roadmap

### Phase 1: Backend foundation
- [x] Spring Boot application, Maven, PostgreSQL
- [x] Database entities
- [x] REST API
- [x] Validation and error handling

### Phase 2: Electricity reporting
- [x] Submit reports, store coordinates, retrieve nearby, timestamps
- [ ] Report history

### Phase 3: Outage detection
- [x] Geographic clustering, time-window analysis, confidence scoring
- [x] Create and update outages, detect restoration

### Phase 4: Frontend
- [ ] Dashboard, interactive map, confidence and affected areas
- [ ] "Is It Just Me?"

### Phase 5: IoT
- [ ] Simulated device events, device auth, device API
- [ ] ESP32 prototype

### Phase 6: Cloud deployment
- [ ] Containerize, deploy, environment config, monitoring, logging, CI/CD

### Phase 7: Advanced analytics
- [ ] Historical analysis, reliability stats, duration analysis, prediction

## Safety

IoT electricity monitoring must never require anyone to touch exposed mains. Use simulated
events during development; any hardware prototype needs isolation, certified components and
qualified supervision.

## Docs

- [How it runs](docs/HOW_IT_RUNS.md)
- [Architecture](docs/ARCHITECTURE.md)

## License

Educational and development use for now.
