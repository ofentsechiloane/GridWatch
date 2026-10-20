# How GridWatch Runs

## Prerequisites

Java 21+, Maven 3.9+, Docker.

## 1. Start the database

```bash
docker run -d --name gridwatch-db \
  -e POSTGRES_DB=gridwatch -e POSTGRES_USER=gridwatch -e POSTGRES_PASSWORD=gridwatch \
  -p 5432:5432 -v gridwatch-data:/var/lib/postgresql/data postgres:16
```

Or, if the Docker Compose plugin is installed: `docker compose up -d`.

Check it is ready:

```bash
docker exec gridwatch-db pg_isready -U gridwatch   # accepting connections
```

Next sessions: `docker start gridwatch-db`. To start it automatically:
`docker update --restart unless-stopped gridwatch-db`.

## 2. Start the API

```bash
mvn spring-boot:run
```

Wait for `Started GridWatchApplication`. It listens on http://localhost:8080.

Connection settings default to the database above. Override with environment variables:
`DB_URL`, `DB_USER`, `DB_PASSWORD`.

## 3. Watch an outage form

Use one location for all examples.

```bash
R='-s -X POST localhost:8080/api/reports -H Content-Type:application/json'
OFF='{"status":"OFF","latitude":-26.2041,"longitude":28.0473}'
ON='{"status":"RESTORED","latitude":-26.2041,"longitude":28.0473}'
```

| Step | Command | Result |
| ---- | ------- | ------ |
| 1 | `curl $R -d "$OFF"` | Report saved. No outage yet. |
| 2 | `curl $R -d "$OFF"` | `POSSIBLE` outage, 2 reports. |
| 3 | `curl $R -d "$OFF"` | `LIKELY`, confidence 0.6. |
| 4 and 5 | two more `OFF` reports | `CONFIRMED`, confidence 1.0. |
| 6 and 7 | two `RESTORED` reports | Outage becomes `RESTORED`. |

After each step:

```bash
curl -s localhost:8080/api/outages          # active outages
curl -s "localhost:8080/api/outages/nearby?lat=-26.2041&lon=28.0473&radius=2000"
curl -s "localhost:8080/api/reports/nearby?lat=-26.2041&lon=28.0473"
curl -s localhost:8080/actuator/health      # {"status":"UP"}
```

Note that `/api/outages` only lists active outages, so a restored outage disappears from it
(look it up by id with `/api/outages/{id}`). An unknown id returns `404`.

Invalid input returns `400`:

```bash
curl -s -X POST localhost:8080/api/reports -H 'Content-Type: application/json' \
  -d '{"status":"OFF","latitude":200,"longitude":28}'
```

## What happens behind the scenes

1. The controller validates the request.
2. The report is saved.
3. Detection looks at recent nearby reports, then creates, updates or closes an outage.
4. Every 5 minutes a background job closes outages with no activity for 3 hours.

Thresholds are in `application.properties`; see [ARCHITECTURE.md](ARCHITECTURE.md).

## Running tests

```bash
mvn test | grep -E "Tests run:|BUILD"
```

Tests use mocks and do not need the database.

## Troubleshooting

| Symptom | Cause and fix |
| ------- | ------------- |
| `Connection to localhost:5432 refused` | Database not running. `docker start gridwatch-db`, then check with `pg_isready`. |
| `unknown shorthand flag: 'd' in -d` | Docker Compose plugin missing. Use `docker run` above, or `sudo apt install docker-compose-v2`. |
| `Port 8080 was already in use` | Another instance is running. `ss -ltnp \| grep 8080`, then `kill <pid>`, or run with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. |
| `curl` prints `000` | The API is not running or is on another port. |
| `401` on an endpoint | The path is not in the public list in `SecurityConfig`. |
| Port 5432 already allocated | A local Postgres is using it. `sudo systemctl stop postgresql`. |
