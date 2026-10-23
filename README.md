# GridWatch

Community-driven electricity outage detection for South Africa. Households report whether
their power is `ON`, `OFF`, `UNSTABLE` or `RESTORED`; GridWatch groups nearby reports in time
and space and decides whether there is a localized outage, and how confident it is.

It answers: *is this just my house, or is the whole area down?*

## Problem statement

South African households and small businesses deal with power interruptions from load
shedding, faults, cable theft and infrastructure failures. When the lights go out, people
can't easily tell what's happening:

- **Is it just my house or the whole area?** A tripped breaker and a failed substation look
  identical from the inside, and the wrong guess costs time and money.
- **Information is slow and fragmented.** Utility and municipal channels often lag behind
  what residents are already experiencing, and neighbours end up asking each other in
  WhatsApp groups and social media posts.
- **No shared, structured picture exists.** Individual reports are scattered, unverified
  and hard to compare across time and place.

## Solution

GridWatch turns scattered household reports into a clear answer.

1. **Report:** anyone submits their power status (`ON`, `OFF`, `UNSTABLE`, `RESTORED`) with
   a location.
2. **Detect:** the backend groups nearby reports in time and space and applies transparent
   rules. Two reports make a `POSSIBLE` outage, three `LIKELY`, and five `CONFIRMED`,
   each with a confidence score. Restoration reports close the outage, and stale outages
   expire on their own.
3. **Answer:** the "Is It Just Me?" endpoint tells you whether an outage covers your
   location, whether neighbours report power is on, or whether there is not enough data yet.

The detection rules are deliberately simple and explainable, so every result can be traced
back to the reports that produced it. Later phases add device-based reporting (simulated
first), a map dashboard and analytics.

## Why we built it

- **A real, local problem.** Power reliability affects daily life in Johannesburg and across
  South Africa, and "is it just me?" is a question people ask every time it happens.
- **Crowdsourcing fits.** Residents are already the fastest source of information about
  their own street. GridWatch gives that information structure instead of leaving it in
  chat groups.
- **A full-stack engineering challenge.** It combines geospatial logic, rule-based detection,
  REST API design, scheduled jobs, validation, testing and CI, with a path to IoT and cloud
  deployment.
- **Room to grow.** The same data can later support reliability statistics, outage
  duration analysis and prediction.

## Status

| Area | State |
| ---- | ----- |
| Reporting API (create, nearby) | Done |
| Rule-based outage detection and restoration | Done |
| Stale outage auto-expiry | Done |
| Outage API (list, by id, nearby) | Done |
| "Is It Just Me?" endpoint | Done |
| Validation and JSON error responses | Done |
| Report history | Planned |
| Auth, devices, frontend, IoT, cloud | Planned |

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

Then ask whether it is just you:

```bash
curl -s "localhost:8080/api/outages/just-me?lat=-26.2041&lon=28.0473"
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
| `GET /api/outages/just-me?lat=&lon=&radius=&minutes=` | Is it just you? Returns a `verdict` of `OUTAGE_IN_AREA`, `LIKELY_JUST_YOU` or `NOT_ENOUGH_DATA` (radius 1-50000 m, default 1000; minutes 1-1440, default 30) |
| `GET /actuator/health` | Health check |

Planned: `GET /api/reports` (history), `/api/auth/*`, `/api/devices/*`.

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
- [x] "Is It Just Me?" endpoint
- [ ] Dashboard, interactive map, confidence and affected areas
- [ ] "Is It Just Me?" screen

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