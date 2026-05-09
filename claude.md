# SnapFix - AI Assistant Context (CLAUDE.md)

---

# Project Overview

SnapFix is a civic infrastructure reporting platform where citizens can report issues such as potholes, garbage, broken streetlights, water leaks, and road damage. Reports include an image and GPS coordinates, are stored with PostGIS geometry, and can be discovered by nearby authenticated users.

This repository contains the **SnapFix Backend (Spring Boot)**.

The long-term product direction is an AI-assisted civic infrastructure management platform with:

- Worker marketplace and bidding
- Task lifecycle management
- Verification workflows
- Payment and wallet system
- AI classification and duplicate detection
- Event-driven architecture

---

# Current Development Stage

| Field | Value |
| ----- | ----- |
| Release | Release 1 - Civic Reporting Foundation |
| Current phase | Release 1 Phase 4 - Discovery and Notifications |
| Completed phases | Phase 1 - Platform Foundation, Phase 2 - Identity and User System, Phase 3 - Civic Report System, Phase 4 - Discovery and Notifications |
| Current status | Release 1 core backend implemented and integration-tested |
| Latest verification | `mvn clean test` passes: 30 tests, 0 failures, 0 errors |

---

# Tech Stack

| Layer | Technology |
| ----- | ---------- |
| Backend | Java 21, Spring Boot 4.0.3, Spring Security, Spring Data JPA |
| Database | PostgreSQL + PostGIS |
| Spatial | Hibernate Spatial + JTS |
| Storage | Cloudinary through `StorageService` |
| DevOps | Docker, Docker Compose |
| Testing | JUnit 5, Spring Boot Test, Testcontainers, MockitoBean, AssertJ |
| Logging/Monitoring | Spring Boot Actuator, SLF4J, Logback, MDC tracing |

---

# Architecture

## Modular Monolith

Current modules:

```text
auth
user
report
notification
worker
storage
geo
common
```

Planned modules:

```text
bid / task / admin / payment / wallet / rating / event / ai / analytics
```

## Module Structure

```text
controller  -> API layer, thin, no business logic
service     -> business logic and transactions
repository  -> persistence only
entity      -> database models
dto         -> request/response models, never expose entities directly
```

## Shared Layer

```text
common/
  entity     -> BaseEntity, Location
  exception  -> GlobalExceptionHandler, ApiError
  util       -> JwtUtil
```

---

# Implemented APIs

## Auth and User - Phase 2

| Method | Endpoint | Auth | Status |
| ------ | -------- | ---- | ------ |
| POST | `/auth/register` | Public | Implemented |
| POST | `/auth/login` | Public | Implemented |
| POST | `/auth/refresh` | Public | Implemented |
| POST | `/auth/logout` | Bearer token recommended | Implemented |
| GET | `/user/me` | Authenticated | Implemented |
| PUT | `/user/profile` | CITIZEN or WORKER | Implemented |

## Reports - Phase 3

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| POST | `/reports` | CITIZEN | Multipart report creation with `image`, `description`, `category`, `lat`, `lng` |
| GET | `/reports/nearby?lat=&lng=&radius=` | Authenticated | Returns reports within radius, sorted by metre-based distance |
| GET | `/reports/{id}` | Authenticated | Returns one report by id |
| POST | `/reports/{id}/support` | CITIZEN | Adds support once per user per report |

`POST /reports` consumes `multipart/form-data`, not JSON.

Required parts/fields:

```text
image       file part
description text, max 1000 chars
category    POTHOLE | STREETLIGHT | GARBAGE | WATER_LEAK | ROAD_DAMAGE
lat         -90 to 90
lng         -180 to 180
```

## Discovery and Notifications - Phase 4

| Method | Endpoint | Auth | Description |
| ------ | -------- | ---- | ----------- |
| GET | `/notifications?unread=true|false` | Authenticated | Returns current user's notifications, optionally filtered by unread/read state |
| PATCH | `/notifications/{id}/read` | Authenticated owner | Marks one notification as read |
| GET | `/workers/reports/nearby?lat=&lng=` | WORKER | Worker discovery endpoint for nearby reports within 5 km |

Notification events currently created:

- `REPORT_CREATED` when a citizen creates a new report.
- `REPORT_SUPPORTED` when a different citizen supports an existing report or duplicate report.

---

# Entity Design

## BaseEntity

| Field | Type | Notes |
| ----- | ---- | ----- |
| id | UUID | Auto-generated primary key |
| createdAt | Instant | Auto-set on persist |
| updatedAt | Instant | Auto-set on update |

## User

| Field | Type | Notes |
| ----- | ---- | ----- |
| id | UUID | Extends BaseEntity |
| email | String | Unique, not null |
| passwordHash | String | BCrypt hashed |
| role | Role | CITIZEN / WORKER / ADMIN |

## CitizenProfile

| Field | Type | Notes |
| ----- | ---- | ----- |
| userId | UUID | Shared primary key with User through `@MapsId` |
| name | String | Required |
| location | Location | Embedded latitude/longitude |
| reportsSubmitted | int | Incremented when a new report row is created |

## WorkerProfile

| Field | Type | Notes |
| ----- | ---- | ----- |
| userId | UUID | Shared primary key with User through `@MapsId` |
| name | String | Required |
| skills | List<String> | `@ElementCollection` |
| rating | Double | Defaults to 0.0 |

## RefreshToken

| Field | Type | Notes |
| ----- | ---- | ----- |
| id | UUID | Auto-generated |
| token | String | Unique, length 1024 |
| user | User | Many-to-one |
| expiryDate | Instant | 7-day expiry |
| revoked | boolean | Defaults false |

## Report

| Field | Type | Notes |
| ----- | ---- | ----- |
| id | UUID | Auto-generated |
| citizenId | UUID | Current user's id |
| imageUrl | String | Cloudinary secure URL returned by `StorageService` |
| description | String | Required, max 1000 chars |
| category | Category | POTHOLE, STREETLIGHT, GARBAGE, WATER_LEAK, ROAD_DAMAGE |
| location | Point | PostGIS `geometry(Point, 4326)` |
| status | ReportStatus | CREATED, IN_PROGRESS, COMPLETED |
| supportCount | int | Starts at 1 for creator support |
| createdAt | Instant | Set during report creation |

## ReportSupport

| Field | Type | Notes |
| ----- | ---- | ----- |
| id | UUID | Auto-generated |
| reportId | UUID | Report id |
| userId | UUID | Supporting user id |
| createdAt | Instant | Support timestamp |

Unique constraint:

```text
(report_id, user_id)
```

This enforces one support per user per report.

## Notification

| Field | Type | Notes |
| ----- | ---- | ----- |
| notificationId | UUID | Auto-generated primary key |
| recipient | User | Notification owner |
| type | NotificationType | REPORT_CREATED, REPORT_SUPPORTED, BID_APPROVED |
| message | String | User-facing event text |
| read | boolean | Defaults false |
| createdAt | Instant | Set on persist |

---

# Security Architecture

## JWT Flow

```text
Request -> JwtAuthFilter -> validate token -> check blacklist -> set Authentication -> controller
```

## Token Strategy

| Token | TTL | Storage |
| ----- | --- | ------- |
| Access token | 15 minutes | Client |
| Refresh token | 7 days | Database |

Important implementation notes:

- JWTs include a `jti` claim so two tokens issued in the same second for the same user are still unique.
- Refresh token column length is 1024 to safely store JWTs.
- Logout revokes the refresh token and blacklists the access token until its natural expiry.
- The blacklist is in-memory for Release 1 and resets on server restart.

## Role Handling

- `CustomUserDetailsService` grants authorities as `ROLE_` + role name.
- Use `@PreAuthorize("hasRole('CITIZEN')")`, not `hasAuthority`.
- Public endpoints under `/auth/**` must remain explicitly permitted in `SecurityConfig`.

---

# Phase 3 Report System Details

## Report Creation Flow

```text
Citizen -> POST /reports multipart
  -> JwtAuthFilter validates CITIZEN token
  -> ReportController extracts multipart fields
  -> ReportService validates fields and image
  -> Geo duplicate query within 50 metres
  -> if same-category duplicate exists:
       add ReportSupport for current user
       increment supportCount
       return existing report with duplicate message
     else:
       upload image through StorageService
       create Report with PostGIS point
       increment CitizenProfile.reportsSubmitted
       create creator ReportSupport
       create REPORT_CREATED notification
       return created report
```

## Duplicate Detection

- Query reports within 50 metres using PostGIS.
- Filter same category in service.
- If same user already supported that report, return 400.
- If a different citizen reports the same issue, support is added to the existing report instead of creating a new row.
- Supporting or duplicate-reporting an existing issue creates a `REPORT_SUPPORTED` notification for the original report owner.

Response message:

```text
Existing report found - your support has been added
```

## Geo Query

Repository query:

```sql
SELECT * FROM reports
WHERE ST_DWithin(
    location::geography,
    ST_MakePoint(:lng, :lat)::geography,
    :radius
)
ORDER BY ST_Distance(
    location::geography,
    ST_MakePoint(:lng, :lat)::geography
)
```

Important:

- Always pass longitude first to `ST_MakePoint`.
- Always use `::geography` for metre-based radius and sorting.
- `GeoUtil.createPoint(lat, lng)` intentionally stores as `Coordinate(lng, lat)`.

---

# Test Coverage

Latest command:

```text
mvn clean test
```

Latest result:

```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Implemented test areas:

- Auth registration, login, refresh rotation, logout revocation and blacklist
- User current profile and profile updates for citizens/workers
- Security 401 behavior for protected endpoints
- Testcontainers PostgreSQL and PostGIS availability
- Cloudinary storage service unit behavior
- Report creation with multipart upload
- Report fetch by id
- Nearby reports sorted by distance
- Support count increment and duplicate prevention
- Duplicate report detection within 50 metres
- Geo query performance with 1,000 seeded reports under 200ms
- Notification creation when reports are created
- Notification retrieval and unread/read filtering
- Mark-as-read ownership protection
- Notification generation when duplicate reporting adds support
- Worker-only nearby report discovery authorization

Notable test infrastructure:

- `BaseIntegrationTest` uses a singleton PostGIS Testcontainers database for stable context reuse.
- HTTP integration tests use Java `HttpClient` instead of RestAssured because RestAssured proxy handling caused environment-specific failures.
- `StorageService` is mocked in report and notification integration tests with `@MockitoBean` so Cloudinary is not called during tests.

---

# Known Issues and Fixes Applied

## Fixed: 403 Instead of 401 for Invalid JWT

`JwtAuthFilter` now writes 401 and returns immediately for invalid, malformed, expired, or blacklisted bearer tokens.

## Fixed: Logout Returned 500

Logout now maps invalid refresh tokens to typed exceptions. Existing refresh tokens are revoked and saved during login/logout flows.

## Fixed: YAML Syntax in `application.properties`

All properties must use flat dot notation, for example:

```properties
jwt.secret=${JWT_SECRET:change-me-dev-secret-at-least-32-bytes-long}
jwt.expiration=${JWT_EXPIRATION:900000}
```

## Fixed: Backend Starting Before DB in Docker

`docker-compose.yml` uses PostgreSQL healthcheck and `condition: service_healthy`.

## Fixed: Access Token Still Valid After Logout

An in-memory token blacklist checks access tokens on every authenticated request.

## Fixed: Testcontainers on Windows

Docker Desktop must be running and Testcontainers must be able to reach the Docker daemon through the configured pipe.

## Fixed: Testcontainers Context Reuse Bug

`BaseIntegrationTest` starts one shared PostGIS container so later integration tests do not reuse a Spring context pointing at a stopped container.

## Fixed: RestAssured Proxy Crash

Security and API integration tests use Java `HttpClient`.

## Fixed: Duplicate Refresh JWTs

`JwtUtil` now adds `jti` to access and refresh tokens. Without this, login and refresh within the same second could generate identical JWT strings.

## Fixed: Refresh Token Column Too Small

`RefreshToken.token` length is now 1024 because JWTs with `jti` can exceed 255 characters.

## Fixed: Worker Skills Lazy Serialization

`UserService` copies worker skills into a plain DTO list inside a transaction before returning responses.

## Fixed: Phase 3 Placeholder Image URL

Report creation now uploads the multipart image through `StorageService` and stores the returned image URL.

## Fixed: Method Security Denials Reaching Generic 500 Handler

`GlobalExceptionHandler` now maps Spring Security access-denied exceptions to 403 JSON responses so `@PreAuthorize` failures do not become generic 500s.

## Fixed: Phase 4 Notification Ownership

`NotificationService.markRead` loads notifications by notification id and current user id, so users cannot mark another user's notification as read. Missing or foreign notifications return 404.

## Fixed: Phase 4 Email Test Fixture Length

Notification integration tests now generate shorter unique emails so Hibernate Validator's email validation does not reject long local parts.

---

# Current Limitations

- Access-token blacklist is in memory and should move to Redis in a later release.
- No database migration tool is configured yet; schema currently relies on Hibernate DDL.
- Secrets are loaded through environment variables. `.env` is ignored and `.env.example` is the committed template.
- `Report.citizenId` is stored as UUID rather than a JPA relationship to `User`; this keeps the module simple but leaves referential integrity unenforced at DB level.
- Report status lifecycle exists as enum only; no worker/task transition engine yet.
- Notifications are stored and retrievable, but real-time push/WebSocket delivery is deferred to later releases.

---

# Developer Plan Updates Recommended

## Release 1 Plan Should Be Updated

Phase 4 should now be marked complete for Release 1's backend scope.

| Phase | Description | Status |
| ----- | ----------- | ------ |
| 1 | Platform Foundation | Done |
| 2 | Identity and User System | Done |
| 3 | Civic Report System | Done |
| 4 | Discovery and Notifications | Done |

Release 1 final checklist should now read:

- [x] Auth flow: register -> login -> JWT -> protected endpoint
- [x] Report creation: image -> Cloudinary, GPS -> PostGIS, fields -> DB
- [x] Nearby search: reports returned sorted by distance
- [x] Support: count increments, duplicate detection in place
- [x] Notifications: stored on report creation/support, retrievable, markable as read
- [ ] All containers start cleanly with `docker-compose up`
- [x] Test coverage target for Release 1 currently satisfied by integration tests

## Phase 4 Scope Completed

- Added `notification` module with entity, repository, service, controller, and DTO.
- Created notifications when a new report is created.
- Created notifications when duplicate reporting or support increments an existing report.
- Added `GET /notifications` with unread/read filtering.
- Added `PATCH /notifications/{id}/read`.
- Added worker discovery endpoint at `GET /workers/reports/nearby`.
- Added integration tests for the Phase 4 flows.

## Recommended Before Release 2

<!-- - Add Flyway or Liquibase before schema grows further. -->
- Rotate any Cloudinary credentials that were previously committed.
- Add a real spatial GiST index for `reports.location`; Hibernate `@Index` does not create the ideal PostGIS spatial index.
- Add DB foreign keys or explicit integrity strategy for `Report.citizenId` and `ReportSupport.reportId/userId`.
- Replace in-memory token blacklist with Redis before scaling beyond one backend instance.
- Decide whether `/auth/refresh` should accept JSON DTO instead of raw text body.
- Add API documentation with request/response examples.

---

# Folder Structure

```text
src/main/java/com/snapfix/
  config/
    SecurityConfig.java
    CloudinaryConfig.java
    RequestLoggingFilter.java
  common/
    entity/
    exception/
    util/
  auth/
    controller/
    service/
    repository/
    entity/
    dto/
    security/
  user/
    controller/
    service/
    repository/
    entity/
    dto/
  report/
    controller/
    service/
    repository/
    entity/
    dto/
  storage/
    service/
    controller/
  geo/
    util/
```

---

# Docker Environment

`docker-compose.yml` runs:

```text
postgres: postgis/postgis:15-3.3 -> port 5432
backend:  build: .               -> port 8080
```

Required backend environment variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/snapfix
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=replace-with-a-random-secret-at-least-32-bytes-long
JWT_EXPIRATION=900000
CLOUDINARY_CLOUD_NAME=replace-with-cloudinary-cloud-name
CLOUDINARY_API_KEY=replace-with-cloudinary-api-key
CLOUDINARY_API_SECRET=replace-with-cloudinary-api-secret
```

Docker rules:

- Use `postgres` as the DB hostname inside Docker, not `localhost`.
- Keep `condition: service_healthy`.
- Use flat dot notation in `.properties`.
- Inspect `docker-compose logs backend` first when debugging startup.

---

# Roadmap

## Release 1 - Civic Reporting Foundation

- Phase 1: Platform Foundation - Done
- Phase 2: Identity and User System - Done
- Phase 3: Civic Report System - Done
- Phase 4: Discovery and Notifications - Done

## Release 2 - Worker Marketplace and Task Assignment

- Worker profile location tracking
- Bidding marketplace
- Admin governance and AdminActionLog
- Task lifecycle engine

## Release 3 - Task Completion, Verification and Payment

- Proof of work upload
- Citizen verification workflow
- Retry logic
- Payment and wallet
- Worker reputation

## Release 4 - AI Intelligence and Event-Driven Architecture

- Kafka event bus
- FastAPI AI service
- Image validation and category classification
- Duplicate detection improvements
- pgvector similarity search
- Priority scoring
- Real-time WebSocket notifications

## Release 5 - Production Hardening and Scalability

- Spring Cloud Gateway
- Redis rate limiting and caching
- Prometheus, Grafana, ELK
- GitHub Actions CI/CD
- Kubernetes with HPA
- k6 load testing
- City analytics dashboard

---

# Non-Functional Targets

| Metric | Target |
| ------ | ------ |
| Reports supported in Phase 3 tests | 1,000 |
| Concurrent users | 100 |
| API response time | < 200ms where practical |
| Geo query | < 200ms on 1,000 seeded reports |
