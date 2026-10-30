# GridWatch

**Submission code: WTC-DPT2PUZT**

A cloud-based community and IoT electricity outage detection platform. Residents and
sensors report power status, GridWatch clusters those reports by location and time, and
shows who is affected on a live map.

- Live app: `https://gridwatch-cktr.onrender.com`
- UI (GitHub Pages): `https://ofentsechiloane.github.io/GridWatch/`
- Repo: https://github.com/ofentsechiloane/GridWatch

---

## 1. Problem statement

In South Africa, power cuts (load shedding, grid faults, cable theft, transformer failures)
are frequent and unpredictable. When the lights go off, people cannot quickly answer basic
questions:

- Is it just my house, or is the whole street out?
- Has anyone reported it? Is it a fault or a scheduled cut?
- When is it likely to come back?

Utility fault lines are overloaded, slow to update, and rarely give area-level information.
There is no simple, real-time, community-driven view of where the power is out right now.

## 2. Story / scenario

It is 19:40 on a winter evening in Johannesburg. **Thandi** is cooking when the power goes
off. She checks the neighbours' windows, but cannot tell if it is her breaker or the area.

1. She opens GridWatch and taps **"Is It Just Me?"**. The app uses her location and
   compares nearby `OFF` and `ON` reports.
2. She submits an `OFF` report. Two neighbours do the same within minutes.
3. GridWatch groups the three reports within 1 km and 30 minutes and raises a **LIKELY**
   outage on the map. Five reports make it **CONFIRMED**.
4. Meanwhile, small IoT devices installed in the area (simulated in this project) send
   `OFF` events automatically, adding independent evidence.
5. An hour later power returns. Reports and devices send `RESTORED`, and GridWatch closes the
   outage. If nobody reports for 3 hours, the outage is closed automatically.

Thandi knows within minutes that it is an area fault, not her house, and the community
gets a shared, trustworthy picture.

## 3. Solution

GridWatch is a REST API plus a web dashboard that turns raw reports into outage
intelligence.

| Feature | What it does |
| ------- | ------------ |
| Community reporting | Anyone submits `ON`, `OFF`, `UNSTABLE` or `RESTORED` with coordinates |
| Outage detection | Clusters nearby reports in a time window and scores confidence |
| Outage lifecycle | `POSSIBLE` → `LIKELY` → `CONFIRMED` → `RESTORED`, with auto-expiry |
| "Is It Just Me?" | Compares your area's reports to say if it is local or widespread |
| Interactive map | Leaflet map showing affected areas and radius |
| Dashboard | Active outages, report counts and confidence, light and dark mode |
| IoT devices | Registered devices post status with an API key (simulated, no mains hardware) |
| Cloud deployment | Dockerised, hosted on Render with managed PostgreSQL, CI on GitHub Actions |
| Monitoring | Health and Prometheus metrics, structured JSON logs |

### Detection rules

| Setting | Default |
| ------- | ------- |
| Cluster radius | 1000 m |
| Time window | 30 min |
| `POSSIBLE` | 2+ reports |
| `LIKELY` | 3+ reports |
| `CONFIRMED` | 5+ reports |
| Restoration | 2+ `ON`/`RESTORED` reports nearby |
| Auto-close | no activity for 180 min |

Confidence = `min(1.0, reportCount / 5)`.

## 4. Tech stack and tools

| Layer | Tools |
| ----- | ----- |
| Language / runtime | Java 21 |
| Backend | Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator) |
| Database | PostgreSQL 16 (H2 for the `local` profile) |
| Frontend | HTML, CSS, JavaScript, Leaflet maps |
| Build | Maven |
| Testing | JUnit 5, Mockito |
| Containers | Docker, Docker Compose |
| CI/CD | GitHub Actions (build and test, Pages deploy), Render auto-deploy |
| Hosting | Render (API), GitHub Pages (UI), managed PostgreSQL |
| Monitoring | Spring Actuator, Micrometer Prometheus, JSON (logstash) logs |
| IoT | Simulated devices (`simulate.sh`); ESP32 prototype planned |
| Dev tools | IntelliJ IDEA, Git, GitHub |

## 5. Architecture

```text
 Residents (browser)            IoT devices (simulated / ESP32)
        |                                   |
        v                                   v
 GitHub Pages UI  ----- HTTPS / JSON ----> Spring Boot API (Render, Docker)
 (Leaflet map,                                   |
  dashboard)                  Controllers  validate input, map HTTP <-> DTOs
                                                 |
                              Services     Report, Outage, OutageDetection,
                                           Device, JustMe
                                                 |
                              Repositories Spring Data JPA
                                                 |
                                           PostgreSQL
                                           reports, outages, devices

 OutageExpiryJob (every 5 min) --> closes stale outages
 Actuator: /actuator/health, /actuator/prometheus (admin key)
 GitHub Actions: build + test on push, auto-deploy to Render when checks pass
```

### Packages (`com.gridwatch`)

| Package | Contents |
| ------- | -------- |
| `config` | `SecurityConfig`, `AdminKeyFilter` |
| `controller` | `ReportController`, `OutageController`, `JustMeController`, `DeviceController` |
| `service` | `ReportService`, `OutageService`, `OutageDetectionService`, `OutageExpiryJob`, `JustMeService`, `DeviceService` |
| `repository` | `ElectricityReportRepository`, `OutageRepository`, `DeviceRepository` |
| `model` | `ElectricityReport`, `Outage`, `Device` and status enums |
| `dto` | request and response objects |
| `util` | `GeoUtils` (haversine distance, bounding box) |
| `exception` | `GlobalExceptionHandler` |

### API

| Method | Endpoint | Purpose |
| ------ | -------- | ------- |
| POST | `/api/reports` | Submit a report |
| GET | `/api/reports`, `/api/reports/nearby` | List and nearby reports |
| GET | `/api/outages`, `/api/outages/{id}`, `/api/outages/nearby` | Outages |
| GET | `/api/outages/just-me` | "Is It Just Me?" check |
| POST | `/api/devices` | Register device (admin key) |
| POST | `/api/devices/events` | Device status event (device API key) |
| GET | `/actuator/health` | Health check |

### Security

Stateless API, CSRF disabled, CORS limited to the Pages origin. Device registration and
metrics need an admin key; devices authenticate with their own API key. The prod profile
refuses to start without `ADMIN_KEY`.

More detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 6. Run it

**Local (H2, no setup)**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Docker (app + PostgreSQL)**

```bash
docker compose up --build
```

Open http://localhost:8080.

**Simulate an outage**

```bash
./simulate.sh
```

**Production environment variables (Render)**

| Variable | Example |
| -------- | ------- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://host:5432/dbname?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | database credentials |
| `ADMIN_KEY` | generated secret |

## 7. Tests

```bash
mvn test | grep -E "Tests run:|BUILD"
```

## 8. Roadmap

- [x] Phase 1: Backend foundation
- [x] Phase 2: Electricity reporting
- [x] Phase 3: Outage detection
- [x] Phase 4: Frontend (map, dashboard, "Is It Just Me?")
- [ ] Phase 5: IoT (simulated devices done, ESP32 prototype pending)
- [ ] Phase 6: Cloud (deployed, monitoring and full CI/CD in progress)
- [ ] Phase 7: Advanced analytics (history, reliability stats, prediction)

## 9. Safety

IoT electricity monitoring must never require anyone to touch exposed mains. Use simulated
events during development; any hardware prototype needs isolation, certified components and
qualified supervision.

## 10. Author

Ofentse Chiloane, WeThinkCode_
GitHub: github.com/ofentsechiloane · LinkedIn: linkedin.com/in/ofentse-chiloane

Educational and development use.