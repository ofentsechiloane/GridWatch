# Architecture

## Overview

A layered Spring Boot REST API backed by PostgreSQL. Detection runs synchronously inside the
request that submits a report, plus a scheduled job that closes stale outages.

```text
Client (curl / future web app)
        |
        v
  Controllers      validate input, map HTTP <-> DTOs
        |
        v
  Services         ReportService, OutageService, OutageDetectionService
        |
        v
  Repositories     Spring Data JPA
        |
        v
  PostgreSQL       electricity_reports, outages

  OutageExpiryJob (scheduled) --> OutageDetectionService.expireStale
```

## Packages (`com.gridwatch`)

| Package | Contents |
| ------- | -------- |
| `config` | `SecurityConfig` |
| `controller` | `ReportController`, `OutageController` |
| `dto` | `ReportRequest`, `ReportResponse`, `OutageResponse` |
| `model` | `ElectricityReport`, `Outage`, `ElectricityStatus`, `OutageStatus` |
| `repository` | `ElectricityReportRepository`, `OutageRepository` |
| `service` | `ReportService`, `OutageService`, `OutageDetectionService`, `OutageExpiryJob` |
| `util` | `GeoUtils` (haversine distance, bounding box) |
| `exception` | `GlobalExceptionHandler`, `ResourceNotFoundException` |

## Data model

**electricity_reports**: id, status (`ON|OFF|UNSTABLE|RESTORED`), latitude, longitude,
description (max 500), createdAt. Indexed on `createdAt` and `(latitude, longitude)`.

**outages**: id, status (`POSSIBLE|LIKELY|CONFIRMED|RESTORED`), centerLatitude,
centerLongitude, radiusMeters, reportCount, confidence (0 to 1), startedAt, updatedAt,
restoredAt (null while active).

Tables are created and updated by Hibernate (`ddl-auto=update`), fine for development.

## Request flow: `POST /api/reports`

1. `ReportController` validates the body (status required, lat in -90..90, lon in -180..180,
   description at most 500 chars). Failures return `400` with a JSON `ApiError`.
2. `ReportService.create` saves the report.
3. `OutageDetectionService.evaluate(report)` runs in the same transaction.
4. The saved report is returned with `201`.

## Detection rules

Settings are in `application.properties` with these defaults.

| Property | Default |
| -------- | ------- |
| `gridwatch.detection.radius-meters` | 1000 |
| `gridwatch.detection.window-minutes` | 30 |
| `gridwatch.detection.min-possible` | 2 |
| `gridwatch.detection.min-likely` | 3 |
| `gridwatch.detection.min-confirmed` | 5 |
| `gridwatch.detection.min-restored` | 2 |
| `gridwatch.expiry.stale-after-minutes` | 180 |

**New `OFF` or `UNSTABLE` report**

1. Find `OFF`/`UNSTABLE` reports inside the time window and within the radius of the new
   report. A bounding-box query narrows candidates in the database; exact haversine distance
   filters them in Java.
2. Fewer than `min-possible`: do nothing.
3. Otherwise compute the cluster centre (average of coordinates) and radius (the larger of
   half the cluster radius and the farthest report from the centre).
4. Status: `CONFIRMED` at 5 or more, `LIKELY` at 3 or more, else `POSSIBLE`.
   Confidence is `min(1.0, count / min-confirmed)`.
5. If an active outage already covers the area (within its radius plus the cluster radius),
   update it; otherwise create a new one.

**New `ON` or `RESTORED` report**

1. Find an active outage near the report; if none, do nothing.
2. If at least `min-restored` `ON`/`RESTORED` reports are nearby in the window, mark the
   outage `RESTORED` and set `restoredAt`.

**Expiry**: `OutageExpiryJob` runs every 5 minutes (`gridwatch.expiry.interval-ms`) and marks
any active outage with no update for `stale-after-minutes` as `RESTORED`.

## Security

Stateless, CSRF disabled, no sessions. `/api/reports/**`, `/api/outages/**`,
`/actuator/health` and `/error` are public; everything else requires authentication.
Login and registration are not implemented yet, so nothing else exists to protect.

## Errors

`GlobalExceptionHandler` returns a consistent JSON body
(`timestamp, status, error, message, details`) for validation errors, missing or invalid
parameters, malformed JSON (`400`) and unknown resources (`404`).

## Known limitations

- Reports have no reporter identity, so one client can create an outage by sending two
  `OFF` reports.
- After an outage is restored, older `OFF` reports are still inside the 30-minute window and
  can immediately recreate it.
- Spatial search uses a bounding box and haversine in Java, not PostGIS.
- Active outages are loaded and filtered in memory; fine at small scale.
- No authentication, rate limiting or device ingestion yet.

## Planned architecture

- **Reporter identity and auth** so detection counts distinct reporters.
- **"Is It Just Me?"** endpoint comparing nearby `OFF` and `ON` reports.
- **IoT ingestion**: devices post status to the API with device authentication; detection
  treats them as an independent evidence source. Simulated devices first, no mains hardware.
- **PostGIS** for spatial queries at scale.
- **Frontend** (React plus a map) reading the outage API.
- **Cloud deployment**: containerized app and managed PostgreSQL, with logging, monitoring
  and CI/CD.

## Evaluation ideas

Detection accuracy, detection latency, geographic accuracy, false positive rate,
scalability and API performance under load.
