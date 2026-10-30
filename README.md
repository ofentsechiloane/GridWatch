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
- [x] Report history

### Phase 3: Outage detection
- [x] Geographic clustering, time-window analysis, confidence scoring
- [x] Create and update outages, detect restoration

### Phase 4: Frontend
- [x] "Is It Just Me?" endpoint
- [x] Interactive map with affected areas (Leaflet)
- [x] Dashboard
- [x] "Is It Just Me?" screen (basic page at `/`)

### Phase 5: IoT
- [x] Simulated device events, device auth, device API
- [ ] ESP32 prototype

### Phase 6: Cloud deployment
- [x] Containerize
- [x] Deploy (Render), environment config, GitHub Pages UI
- [ ] Monitoring, logging, full CI/CD

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