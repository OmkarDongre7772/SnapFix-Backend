# SnapFix Backend

SnapFix is a civic infrastructure reporting backend. Citizens can report issues such as potholes, garbage, broken streetlights, water leaks and road damage with an image and GPS coordinates. Reports are stored with PostGIS geometry, can be discovered by nearby users/workers, can receive support from other citizens, and now generate stored notifications.

This repository contains the Spring Boot backend for **Release 1 - Civic Reporting Foundation**.

## Release Status

| Area | Status |
| ---- | ------ |
| Phase 1 - Platform Foundation | Complete |
| Phase 2 - Identity and User System | Complete |
| Phase 3 - Civic Report System | Complete |
| Phase 4 - Discovery and Notifications | Complete |
| Latest test result | `mvn clean test` passes: 30 tests |

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL with PostGIS
- Hibernate Spatial and JTS
- Cloudinary image storage
- Docker and Docker Compose
- JUnit 5, Testcontainers, AssertJ and Mockito

## Features

- Register/login users as `CITIZEN`, `WORKER` or `ADMIN`
- JWT access tokens and refresh-token rotation
- Logout with refresh-token revocation and in-memory access-token blacklist
- Citizen and worker profile APIs
- Multipart report creation with image upload
- PostGIS-backed nearby report search using metre-based geography queries
- Duplicate report detection within 50 metres by category
- One support per user per report
- Stored notifications for report creation and support events
- Read/unread notification filtering and mark-as-read
- Worker-only nearby report discovery
- Structured exception responses for validation, authentication and authorization failures

## Project Structure

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

## Requirements

- Java 21
- Maven, or the included Maven wrapper
- Docker Desktop for integration tests and local PostgreSQL/PostGIS
- Cloudinary credentials for real image uploads

## Configuration

For local development, the app reads `src/main/resources/application.properties`.

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

For Docker Compose, the backend should connect to PostgreSQL through the Docker service name:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/snapfix
```

Do not commit real production secrets. Use `.env` locally and keep `.env.example` as the committed template.

## Run Locally

Start PostgreSQL/PostGIS and backend with Docker Compose:

```powershell
docker-compose up --build
```

Or run the backend directly after PostgreSQL is available:

```powershell
.\mvnw spring-boot:run
```

Health check:

```text
GET /actuator/health
```

## Run Tests

Docker Desktop must be running because integration tests use Testcontainers with PostGIS.

```powershell
.\mvnw clean test
```

Latest verified result:

```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## API Overview

### Auth

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| POST | `/auth/register` | Public | Register a citizen, worker or admin |
| POST | `/auth/login` | Public | Login and receive access/refresh tokens |
| POST | `/auth/refresh` | Public | Rotate refresh token and issue new tokens |
| POST | `/auth/logout` | Bearer token | Revoke refresh token and blacklist access token |

### User

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| GET | `/user/me` | Authenticated | Get current user and role-specific profile |
| PUT | `/user/profile` | Authenticated | Update citizen or worker profile |

### Reports

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| POST | `/reports` | CITIZEN | Create a multipart report with image and location |
| GET | `/reports/nearby?lat=&lng=&radius=` | Authenticated | Find reports within a radius, sorted by distance |
| GET | `/reports/{id}` | Authenticated | Get one report |
| POST | `/reports/{id}/support` | CITIZEN | Support a report once |

`POST /reports` expects `multipart/form-data`:

```text
image       file
description text, max 1000 chars
category    POTHOLE | STREETLIGHT | GARBAGE | WATER_LEAK | ROAD_DAMAGE
lat         latitude
lng         longitude
```

### Notifications

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| GET | `/notifications?unread=true` | Authenticated | List current user's notifications |
| PATCH | `/notifications/{id}/read` | Notification owner | Mark a notification as read |

### Worker Discovery

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| GET | `/workers/reports/nearby?lat=&lng=` | WORKER | Find reports within 5 km for workers |

## Report Flow

1. Citizen sends `POST /reports` with image, category, description and coordinates.
2. The service searches for same-category reports within 50 metres.
3. If a duplicate exists, SnapFix adds support to the existing report and creates a `REPORT_SUPPORTED` notification.
4. If no duplicate exists, SnapFix uploads the image, stores the PostGIS point, creates the report, creates creator support, increments `reportsSubmitted`, and creates a `REPORT_CREATED` notification.

## Current Known Limitations

- Secrets are loaded through environment variables. `.env` is ignored; `.env.example` is the committed template.
- Schema management uses Hibernate DDL; Flyway or Liquibase should be added before Release 2.
- Access-token blacklist is in memory and should move to Redis before multi-instance deployment.
- Notifications are persisted, but real-time push/WebSocket delivery is not part of Release 1.
- `reports.location` should receive an explicit PostGIS GiST index for production scale.
- `Report.citizenId` and report support user/report ids are UUID fields rather than full database foreign-key relationships.
- Report status lifecycle exists as an enum, but assignment and completion workflows are planned for later releases.
- A Docker Compose smoke test should be recorded before tagging the release.

## Roadmap

### Release 2 - Worker Marketplace and Task Assignment

- Worker location tracking
- Bidding marketplace
- Admin governance
- Task assignment lifecycle

### Release 3 - Completion, Verification and Payment

- Proof-of-work uploads
- Citizen verification
- Payment and wallet
- Worker ratings

### Release 4 - AI and Event-Driven Architecture

- Kafka event bus
- AI image/category validation
- Duplicate detection improvements
- Real-time notification delivery

### Release 5 - Production Hardening

- API gateway
- Redis rate limiting/caching
- Observability stack
- CI/CD
- Kubernetes deployment
