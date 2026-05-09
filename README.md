
# SnapFix Backend

> Civic infrastructure platform with geospatial search, JWT security, PostGIS-powered discovery, and a roadmap toward event-driven AI architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PostGIS-blue)
![Tests](https://img.shields.io/badge/Tests-30_Passing-success)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![Release](https://img.shields.io/badge/Release-v1.0.0-purple)

---

# Overview

SnapFix is a civic infrastructure reporting backend where citizens can report issues such as:

- Potholes
- Garbage accumulation
- Broken streetlights
- Water leaks
- Road damage

Reports include:
- GPS coordinates
- Uploaded images
- Category classification
- Nearby discovery support
- Duplicate detection
- Citizen support counts
- Stored notifications

This repository contains the Spring Boot backend for:

# Release 1 — Civic Reporting Foundation

---

# Motivation

SnapFix was built to explore:

- Geospatial backend engineering
- PostGIS-powered discovery systems
- JWT authentication and authorization
- Real-world civic workflows
- Modular monolith architecture
- Scalable backend evolution toward distributed systems

The long-term goal is to evolve SnapFix from a modular monolith into a production-grade event-driven platform with AI-assisted infrastructure analysis.

---

# Release Status

| Area | Status |
|---|---|
| Phase 1 — Platform Foundation | ✅ Complete |
| Phase 2 — Identity and User System | ✅ Complete |
| Phase 3 — Civic Report System | ✅ Complete |
| Phase 4 — Discovery and Notifications | ✅ Complete |
| Latest Test Result | ✅ `mvn clean test` — 30 tests passing |

---

# Architecture

```mermaid
flowchart LR

    Citizen[Citizen App]
    Worker[Worker App]
    Admin[Admin Dashboard]

    Citizen --> API
    Worker --> API
    Admin --> API

    API[Spring Boot Backend]

    API --> Auth[Auth Module]
    API --> Report[Report Module]
    API --> Notification[Notification Module]
    API --> WorkerModule[Worker Module]
    API --> Storage[Storage Module]
    API --> Geo[Geo Module]

    Auth --> DB[(PostgreSQL + PostGIS)]
    Report --> DB
    Notification --> DB
    WorkerModule --> DB

    Storage --> Cloudinary[(Cloudinary)]

    Geo --> PostGIS[(PostGIS Geography Queries)]
```

---

# Release Roadmap

```mermaid
timeline
    title SnapFix Platform Evolution

    Release 1 : Civic Reporting Foundation
              : JWT Authentication
              : PostGIS Nearby Search
              : Notifications

    Release 2 : Worker Marketplace
              : Bidding System
              : Task Assignment

    Release 3 : Verification & Payments
              : Proof Uploads
              : Wallet System
              : Ratings

    Release 4 : AI + Event Architecture
              : Kafka
              : FastAPI AI Service
              : WebSockets

    Release 5 : Production Hardening
              : Kubernetes
              : Observability
              : Load Testing
```

---

# Engineering Highlights

- PostGIS metre-based geo queries using `ST_DWithin`
- JWT authentication with refresh-token rotation
- Multipart image upload pipeline with Cloudinary
- Duplicate report detection within 50 metres
- One support per user per report enforcement
- Role-based access control (`CITIZEN`, `WORKER`, `ADMIN`)
- Structured validation and exception responses
- Dockerized local development
- PostgreSQL/PostGIS integration tests using Testcontainers
- Modular monolith architecture designed for future microservice extraction

---

# Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 4.0.3 |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA |
| Database | PostgreSQL + PostGIS |
| Geospatial Support | Hibernate Spatial + JTS |
| Image Storage | Cloudinary |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + AssertJ + Testcontainers |

---

# Features

- Register/login users as `CITIZEN`, `WORKER`, or `ADMIN`
- JWT access tokens and refresh-token rotation
- Logout with refresh-token revocation and access-token blacklist
- Citizen and worker profile APIs
- Multipart report creation with image upload
- Nearby report discovery using PostGIS geography queries
- Duplicate report detection within 50 metres
- One support per user per report
- Stored notifications for report events
- Read/unread notification filtering
- Worker-only nearby report discovery
- Structured validation and authorization error responses

---

# Report Lifecycle

```mermaid
flowchart TD

    A[Citizen Creates Report]
    B{Duplicate Within 50m?}

    A --> B

    B -->|Yes| C[Add Support To Existing Report]
    B -->|No| D[Upload Image To Cloudinary]

    D --> E[Store Report In PostgreSQL + PostGIS]
    E --> F[Create Notifications]
    F --> G[Nearby Workers Discover Report]

    C --> H[REPORT_SUPPORTED Notification]
```

---

# Authentication Flow

```mermaid
sequenceDiagram

    participant User
    participant Backend
    participant Database

    User->>Backend: POST /auth/login
    Backend->>Database: Validate credentials
    Database-->>Backend: User found

    Backend-->>User: Access Token + Refresh Token

    User->>Backend: Protected Request + JWT
    Backend->>Backend: Validate JWT

    Backend-->>User: Authorized Response
```

---

# Nearby Search Logic

```mermaid
flowchart LR

    UserLocation[User Coordinates]
    Radius[Radius in Metres]
    Reports[(Reports Table)]

    UserLocation --> Query
    Radius --> Query

    Query[ST_DWithin Geography Query]

    Query --> Reports

    Reports --> Sorted[Results Sorted By Distance]
```

---

# Notification Flow

```mermaid
flowchart TD

    ReportCreated[Report Created]
    ReportSupported[Report Supported]

    ReportCreated --> NotificationService
    ReportSupported --> NotificationService

    NotificationService --> Database[(Notifications Table)]

    Database --> User[Authenticated User]
```

---

# Core Entity Relationships

```mermaid
erDiagram

    USER ||--o{ REPORT : creates
    USER ||--o{ NOTIFICATION : receives
    USER ||--o{ REPORT_SUPPORT : supports

    USER {
        UUID id
        String email
        String role
    }

    REPORT {
        UUID id
        UUID citizenId
        String category
        Point location
        String imageUrl
    }

    NOTIFICATION {
        UUID id
        UUID recipientId
        String type
        boolean read
    }
```

---

# Project Structure

```text
src/main/java/com/snapfix/

  auth/           authentication, JWT, refresh tokens
  user/           users, citizen profiles, worker profiles
  report/         civic reports, duplicate detection, support
  notification/   stored notifications and read state
  worker/         worker discovery endpoints
  storage/        Cloudinary image upload abstraction
  geo/            PostGIS/JTS helpers
  common/         shared entities, exceptions and utilities
  config/         security, Cloudinary and request logging config
```

---

# Performance Targets

| Metric | Target |
|---|---|
| API Response Time | < 200ms |
| Nearby Search Query | < 200ms |
| Concurrent Users | 100 |
| Test Coverage | 70%+ |

---

# Requirements

- Java 21
- Maven or Maven Wrapper
- Docker Desktop
- PostgreSQL/PostGIS
- Cloudinary credentials

---

# Configuration

Local configuration is loaded from:

```text
src/main/resources/application.properties
```

Important configuration keys:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/snapfix
spring.datasource.username=postgres
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:password}

jwt.secret=${JWT_SECRET:change-me-dev-secret-at-least-32-bytes-long}
jwt.expiration=${JWT_EXPIRATION:900000}

cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:dev-cloud-name}
cloudinary.api-key=${CLOUDINARY_API_KEY:dev-api-key}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:dev-api-secret}
```

Docker Compose datasource:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/snapfix
```

> Never commit production secrets. Use `.env` locally and commit only `.env.example`.

---

# Local Development Infrastructure

```mermaid
flowchart LR

    Docker[Docker Compose]

    Docker --> Backend[Spring Boot Backend]
    Docker --> Postgres[(PostgreSQL + PostGIS)]

    Backend --> Cloudinary[(Cloudinary)]
```

---

# Run Locally

Start backend + PostgreSQL/PostGIS:

```powershell
docker-compose up --build
```

Or run backend directly:

```powershell
.\mvnw spring-boot:run
```

Health check endpoint:

```text
GET /actuator/health
```

---

# Run Tests

Docker Desktop must be running because integration tests use Testcontainers with PostgreSQL/PostGIS.

```powershell
.\mvnw clean test
```

Latest verified result:

```text
Tests run: 30
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

# API Documentation

Swagger/OpenAPI integration is planned for a future release.

Future endpoint:

```text
/swagger-ui.html
```

---

# API Overview

## Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register citizen, worker, or admin |
| POST | `/auth/login` | Public | Login and receive tokens |
| POST | `/auth/refresh` | Public | Rotate refresh token |
| POST | `/auth/logout` | Bearer token | Logout and revoke tokens |

---

## User

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/user/me` | Authenticated | Get current user profile |
| PUT | `/user/profile` | Authenticated | Update citizen/worker profile |

---

## Reports

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/reports` | CITIZEN | Create multipart report |
| GET | `/reports/nearby?lat=&lng=&radius=` | Authenticated | Nearby report search |
| GET | `/reports/{id}` | Authenticated | Get report |
| POST | `/reports/{id}/support` | CITIZEN | Support report |

`POST /reports` expects:

```text
image       file
description text
category    POTHOLE | STREETLIGHT | GARBAGE | WATER_LEAK | ROAD_DAMAGE
lat         latitude
lng         longitude
```

---

## Notifications

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/notifications?unread=true` | Authenticated | List notifications |
| PATCH | `/notifications/{id}/read` | Owner only | Mark notification as read |

---

## Worker Discovery

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/workers/reports/nearby?lat=&lng=` | WORKER | Nearby worker report search |

---

# Example Geospatial Query

```sql
SELECT *
FROM reports
WHERE ST_DWithin(
    location::geography,
    ST_MakePoint(:lng, :lat)::geography,
    :radiusMetres
)
ORDER BY ST_Distance(
    location::geography,
    ST_MakePoint(:lng, :lat)::geography
);
```

---

# Example Production Index

```sql
CREATE INDEX idx_reports_location
ON reports
USING GIST(location);
```

---

# Report Flow

1. Citizen sends `POST /reports`
2. SnapFix checks for duplicates within 50 metres
3. Duplicate found → support added to existing report
4. No duplicate → image uploaded to Cloudinary
5. Report stored with PostGIS point
6. Notifications created
7. Nearby workers can discover report

---

# Current Known Limitations

- `.env` is ignored; `.env.example` is committed
- Hibernate DDL is used instead of Flyway/Liquibase
- Access-token blacklist is currently in-memory
- WebSocket push notifications are planned for Release 4
- `reports.location` still needs explicit GiST indexing
- Some UUID relationships are not yet full foreign keys
- Assignment/task workflows are planned for Release 2
- Docker Compose smoke test still needs recording

---

# Planned Event-Driven Architecture (Release 4)

```mermaid
flowchart LR

    ReportService --> Kafka[(Kafka)]

    Kafka --> NotificationService
    Kafka --> AIService
    Kafka --> WorkerService

    AIService[FastAPI AI Service]

    AIService --> Kafka

    Kafka --> Backend[(Spring Boot Backend)]
```

---

# Roadmap

## Release 2 — Worker Marketplace and Task Assignment

- Worker location tracking
- Bidding marketplace
- Admin governance
- Task assignment lifecycle

---

## Release 3 — Completion, Verification and Payment

- Proof-of-work uploads
- Citizen verification
- Payment and wallet
- Worker ratings

---

## Release 4 — AI and Event-Driven Architecture

- Kafka event bus
- AI image/category validation
- Duplicate detection improvements
- Real-time notification delivery

---

## Release 5 — Production Hardening

- API gateway
- Redis rate limiting/caching
- Observability stack
- CI/CD automation
- Kubernetes deployment
- Load testing
- Analytics dashboard

---

# Future Goals

- Distributed microservice extraction
- Event sourcing experimentation
- AI-assisted report moderation
- Real-time city analytics
- Kubernetes autoscaling

---

# License

This project is currently intended for educational and portfolio purposes.
