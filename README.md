# GridWatch

## Cloud-Based Community and IoT Electricity Outage Detection Platform

GridWatch is a cloud-based platform designed to detect, analyze, and visualize localized
electricity outages and power instability using community reports, geographic analysis, and
optional IoT devices.

The platform addresses a common problem in South Africa: determining whether a power 
interruption is an isolated household problem, a localized fault, or part of a wider
electricity disruption.

---

## Problem Statement

Electricity interruptions can occur for many reasons, including scheduled maintenance, 
infrastructure failures, localized faults, equipment problems, and broader grid instability.

When electricity goes off, users often have limited information about:

* Whether the problem affects only their property
* Whether nearby households are experiencing the same issue
* How widespread the outage is
* Whether the interruption is likely to be temporary or extended
* When electricity is likely to be restored

Existing information sources may not provide sufficiently localized or real-time information.

GridWatch aims to provide a community-driven source of localized electricity disruption
information.

---

## Proposed Solution

GridWatch collects electricity status reports from users and optional IoT monitoring devices.

The cloud platform analyzes reports based on:

* Geographic proximity
* Time of occurrence
* Electricity status
* Number of nearby reports
* Report reliability
* Optional IoT sensor data

When multiple reports indicate a disruption within a specific geographic area and time period,
GridWatch can identify a potential outage event.

The system then presents the information through an API and, eventually, a web-based map and 
dashboard.

---

## Core Features

### Community Reporting

Users can report their current electricity status:

* `ON` — Electricity is available
* `OFF` — Electricity is unavailable
* `UNSTABLE` — Electricity is experiencing instability
* `RESTORED` — Electricity has returned

Each report can contain:

* Geographic coordinates
* Electricity status
* Timestamp
* Optional description

---

### Outage Detection

GridWatch analyzes recent reports to identify potential outage events.

For example:

```text
User A → Electricity OFF
User B → Electricity OFF
User C → Electricity OFF
User D → Electricity OFF

       ↓

Reports are geographically close
       +
Reports occurred within a short time period
       ↓

Potential outage detected
```

The system assigns a confidence score to detected outages.

---

### "Is It Just Me?"

One of GridWatch's primary features is determining whether an electricity problem is isolated.

For example:

```text
Your electricity: OFF

Nearby reports:
Household A → ON
Household B → ON
Household C → ON

Result:
Possible isolated issue
```

Alternatively:

```text
Your electricity: OFF

Nearby reports:
Household A → OFF
Household B → OFF
Household C → OFF

Result:
Likely localized outage
```

---

### Geographic Analysis

GridWatch uses geographic coordinates to group reports that occur within a specific area.

This allows the system to estimate:

* Outage location
* Approximate affected radius
* Number of affected reports
* Geographic concentration
* Potentially affected users

PostgreSQL with PostGIS can be used for advanced geographic queries.

---

### IoT Integration

GridWatch can optionally receive electricity status information from IoT devices.

A possible architecture is:

```text
IoT Device
    ↓
Internet
    ↓
Cloud API / IoT Gateway
    ↓
GridWatch Backend
    ↓
Outage Detection Engine
    ↓
Database
    ↓
Dashboard / Notifications
```

IoT integration is intended to provide automated evidence that complements community reports.

---

## System Architecture

```text
                   ┌─────────────────────┐
                   │       Users         │
                   │ Web / Mobile Client │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │     REST API        │
                   │   Spring Boot       │
                   └──────────┬──────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
                ▼             ▼             ▼
        ┌──────────────┐ ┌──────────┐ ┌──────────────┐
        │ PostgreSQL   │ │ Detection│ │ Notification │
        │ + PostGIS    │ │ Engine   │ │   Service    │
        └──────────────┘ └────┬─────┘ └──────────────┘
                              │
                              ▼
                       ┌─────────────┐
                       │ Outage Data │
                       │ & Analytics │
                       └─────────────┘

          Optional IoT Devices
                   │
                   ▼
              Cloud Ingestion
                   │
                   └──────────────► REST API
```

---

## Outage Classification

| Status      | Meaning                                       |
| ----------- | --------------------------------------------- |
| `NORMAL`    | No significant disruption detected            |
| `POSSIBLE`  | Some evidence of an electricity disruption    |
| `LIKELY`    | Multiple nearby reports indicate a disruption |
| `CONFIRMED` | Strong evidence from multiple sources         |
| `RESTORED`  | Electricity has returned                      |

The exact classification rules may evolve as the detection system develops.

---

## Outage Detection Model

The initial version of GridWatch will use a rule-based detection system.

A simplified detection process:

```text
New electricity report
        ↓
Find recent nearby reports
        ↓
Check number of reports
        ↓
Check geographic proximity
        ↓
Check time consistency
        ↓
Check optional IoT evidence
        ↓
Calculate confidence score
        ↓
Create or update outage
```

A future version may use machine learning to improve outage prediction and classification.

---

## Technology Stack

### Backend

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* Bean Validation
* Maven

### Database

* PostgreSQL
* PostGIS

### IoT

Potential hardware:

* ESP32
* Isolated voltage/power monitoring sensors
* MQTT or HTTPS communication

### Frontend

A future frontend may use:

* React
* JavaScript / TypeScript
* Mapping APIs
* REST API integration

### Cloud

Potential cloud services include:

* Application hosting
* Managed PostgreSQL
* Object storage
* Monitoring
* Logging
* Notification services

The exact cloud provider will be selected during the deployment phase.

---

## Project Structure

```text
GridWatch/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── gridwatch/
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── detection/
│   │   │           ├── dto/
│   │   │           ├── exception/
│   │   │           ├── model/
│   │   │           ├── repository/
│   │   │           └── service/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── gridwatch/
│
├── iot/
│   ├── firmware/
│   └── device-config/
│
├── infrastructure/
│   ├── cloud/
│   └── deployment/
│
├── docs/
│
├── pom.xml
├── README.md
└── .gitignore
```

---

## Planned API

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

### Electricity Reports

```text
POST /api/reports
GET  /api/reports/nearby
```

### Outages

```text
GET /api/outages
GET /api/outages/{id}
GET /api/outages/nearby
```

### IoT Devices

```text
POST /api/devices
GET  /api/devices
POST /api/devices/{id}/status
```

### System Health

```text
GET /api/health
```

---

## Example Electricity Report

```json
{
  "status": "OFF",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "description": "Electricity went off at approximately 18:42"
}
```

---

## Example Detection Scenario

Consider several users reporting electricity loss within a small geographic area.

```text
18:42 — User A reports OFF
18:44 — User B reports OFF
18:45 — User C reports OFF
18:47 — IoT Device D reports OFF
```

GridWatch analyzes the events and determines that:

* The reports are geographically close.
* The reports occurred within a short time window.
* Multiple independent sources indicate the same condition.

The system may therefore create an outage event with a high confidence score.

---

## South African Context

GridWatch is designed with the South African electricity environment in mind.

Electricity interruptions may have different causes, including:

* Scheduled interruptions
* Infrastructure maintenance
* Equipment failures
* Local distribution faults
* Unplanned outages
* Grid instability

GridWatch does not automatically assume that every electricity interruption is load-shedding.

The platform instead focuses on collecting evidence and identifying localized electricity disruptions.

---

## Privacy and Security

GridWatch should follow privacy-conscious design principles.

Potential measures include:

* Secure authentication
* Password hashing
* HTTPS
* API authorization
* Input validation
* Rate limiting
* Minimal collection of personal information
* Protection of precise location information
* Secure IoT device authentication

Location data should only be collected when necessary for outage detection and geographic analysis.

---

## Safety Considerations

IoT electricity monitoring must be designed with electrical safety as a priority.

The project should not require users or students to directly interact with exposed mains electricity.

Prototype devices should use appropriate electrical isolation, certified components, safe enclosures, and qualified supervision where required.

During early development, simulated IoT events can be used instead of connecting hardware to mains electricity.

---

## Development Roadmap

### Phase 1 — Backend Foundation

* [ ] Create Spring Boot application
* [ ] Configure Maven
* [ ] Configure PostgreSQL
* [ ] Create database entities
* [ ] Implement REST API
* [ ] Add validation
* [ ] Add basic error handling

### Phase 2 — Electricity Reporting

* [ ] Submit electricity reports
* [ ] Store geographic coordinates
* [ ] Retrieve nearby reports
* [ ] Track report timestamps
* [ ] Add report history

### Phase 3 — Outage Detection

* [ ] Implement geographic clustering
* [ ] Implement time-window analysis
* [ ] Implement confidence scoring
* [ ] Create outage events
* [ ] Update outage status
* [ ] Detect restoration

### Phase 4 — Frontend

* [ ] Build dashboard
* [ ] Add interactive outage map
* [ ] Display outage confidence
* [ ] Display affected areas
* [ ] Add "Is It Just Me?" feature

### Phase 5 — IoT

* [ ] Build ESP32 prototype
* [ ] Implement simulated electricity states
* [ ] Connect device to cloud API
* [ ] Authenticate devices
* [ ] Process IoT events

### Phase 6 — Cloud Deployment

* [ ] Containerize application
* [ ] Deploy backend
* [ ] Deploy PostgreSQL
* [ ] Configure environment variables
* [ ] Configure monitoring
* [ ] Configure logging
* [ ] Configure CI/CD

### Phase 7 — Advanced Analytics

* [ ] Historical outage analysis
* [ ] Reliability statistics
* [ ] Outage duration analysis
* [ ] Geographic trends
* [ ] Predictive analytics
* [ ] Machine learning experimentation

---

## Evaluation

The project can be evaluated using metrics such as:

### Detection Accuracy

How accurately does GridWatch identify actual outages?

### Detection Latency

How quickly can the platform identify a potential outage after the first reports arrive?

### Geographic Accuracy

How accurately does the detected outage area represent the affected area?

### False Positive Rate

How often does the system incorrectly classify an isolated issue as a wider outage?

### System Scalability

How does the system perform as the number of users, reports, and IoT devices increases?

### Cloud Performance

Evaluate:

* Response time
* Availability
* Resource utilization
* Database performance
* Scalability

---

## Project Status

GridWatch is currently under active development.

The initial implementation focuses on establishing the Spring Boot backend, database architecture, electricity reporting, and outage detection engine.

---

## Future Development

Potential future capabilities include:

* Real-time outage maps
* Mobile applications
* Push notifications
* SMS notifications
* IoT device networks
* Historical reliability analysis
* Utility integration
* Machine learning-based prediction
* Automated outage restoration detection
* Community reliability scoring
* Cloud-based analytics dashboards

---

## Contributors

GridWatch is developed as a cloud computing project focused on applying distributed systems, cloud technologies, geographic analysis, and IoT concepts to a real-world electricity infrastructure problem.

---

## License

This project is currently intended for educational and development purposes.

A formal open-source license can be added when the project is ready for public distribution.
