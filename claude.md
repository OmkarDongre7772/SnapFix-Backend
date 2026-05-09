# SnapFix – AI Assistant Context (CLAUDE.md)

---

# Project Overview

SnapFix is a civic infrastructure reporting platform where citizens can report issues such as
potholes, garbage, broken streetlights, and water leaks. Reports include images and geolocation
and are discoverable by nearby users and workers.

The system evolves into an AI-assisted civic infrastructure management platform with:

- Worker marketplace & bidding
- Task lifecycle management
- Verification workflows
- Payment system
- AI classification & duplicate detection
- Event-driven architecture

This repository contains the **SnapFix Backend (Spring Boot)**.

---

# Current Development Stage

| Field   | Value                                  |
| ------- | -------------------------------------- |
| Release | Release 1 – Civic Reporting Foundation |
| Phase   | Phase 3 – Civic Report System          |
| Status  | ✅ Phase 1-3 Core Testing Implemented   |

---

# Tech Stack

| Layer              | Technology                                             |
| ------------------ | ------------------------------------------------------ |
| Backend            | Java 21, Spring Boot, Spring Security, Spring Data JPA |
| Database           | PostgreSQL + PostGIS                                   |
| Storage            | Cloudinary                                             |
| DevOps             | Docker, Docker Compose                                 |
| Logging/Monitoring | Spring Boot Actuator, SLF4J, Logback                   |

---

# Architecture

## Modular Monolith

### Current Modules

```
auth
user
report
notification
storage
geo
```

### Future Modules (Release 2+)

```
worker / bid / task / admin / payment / wallet / rating / event / ai / analytics
```

## Module Structure

Each module follows:

```
controller  → API layer (thin, no business logic)
service     → business logic
repository  → persistence only
entity      → database models
dto         → request/response models (never expose entities directly)
```

## Shared Layer

```
common/
  entity    → BaseEntity, Location
  exception → GlobalExceptionHandler
  dto       → ApiResponse wrappers
  util      → JwtUtil, helpers
```

---

# Entity Design

## BaseEntity (abstract)

| Field     | Type    | Notes               |
| --------- | ------- | ------------------- |
| id        | UUID    | Auto-generated PK   |
| createdAt | Instant | Auto-set on persist |
| updatedAt | Instant | Auto-set on update  |

## User

| Field        | Type        | Notes                    |
| ------------ | ----------- | ------------------------ |
| id           | UUID        | Extends BaseEntity       |
| email        | String      | Unique, not null         |
| passwordHash | String      | BCrypt hashed            |
| role         | Role (enum) | CITIZEN / WORKER / ADMIN |

## CitizenProfile

| Field            | Type     | Notes               |
| ---------------- | -------- | ------------------- |
| userId           | UUID     | Shared PK (@MapsId) |
| name             | String   |                     |
| location         | Location | Embedded            |
| reportsSubmitted | int      | Counter             |

## WorkerProfile

| Field  | Type         | Notes                    |
| ------ | ------------ | ------------------------ |
| userId | UUID         | Shared PK (@MapsId)      |
| name   | String       |                          |
| skills | List<String> | @ElementCollection       |
| rating | Double       | Aggregate, updated in R3 |

## RefreshToken

| Field      | Type    | Notes         |
| ---------- | ------- | ------------- |
| id         | UUID    |               |
| token      | String  | Unique        |
| user       | User    | @ManyToOne    |
| expiryDate | Instant |               |
| revoked    | boolean | Default false |

---

# Security Architecture

## JWT Flow

```
Request → JwtAuthFilter → Validate Token → Set Authentication → Controller
```

## Token Strategy

| Token Type    | TTL    | Storage |
| ------------- | ------ | ------- |
| Access Token  | 15 min | Client  |
| Refresh Token | 7 days | DB      |

## Role Handling

- `CustomUserDetailsService` uses `.roles(user.getRole().name())`
- Spring auto-prefixes to `ROLE_CITIZEN`, `ROLE_WORKER`, `ROLE_ADMIN`
- Use `@PreAuthorize("hasRole('CITIZEN')")` — NOT `hasAuthority`
- Mixing `hasRole` and `hasAuthority` causes silent auth failures

---

# Current Progress

## ✅ Phase 1 – Platform Foundation (Completed)

- Spring Boot initialized, modular monolith architecture
- BaseEntity with auditing fields
- PostgreSQL + PostGIS configured, Dockerized
- Spring Boot Actuator — `/actuator/health` working
- Cloudinary integration complete
- GlobalExceptionHandler with structured JSON error responses
- SLF4J + Logback with MDC tracing

---

## ✅ Phase 2 – Identity & User System (Completed)

### APIs Implemented

| Method | Endpoint       | Status |
| ------ | -------------- | ------ |
| POST   | /auth/register | ✅     |
| POST   | /auth/login    | ✅     |
| POST   | /auth/refresh  | ✅     |
| POST   | /auth/logout   | ✅     |
| GET    | /user/me       | ✅     |
| PUT    | /user/profile  | ✅     |

### Security Features

- Password hashing (BCrypt)
- JWT validation filter (JwtAuthFilter)
- Stateless session management
- Refresh token rotation
- Refresh token revocation on logout
- Role-based access via @PreAuthorize
- Custom AuthenticationEntryPoint (401)
- Custom AccessDeniedHandler (403)
- Input validation via @Valid + @NotBlank / @Email / @Size

---

## 🚧 Phase 3 – Civic Report System (Not Started)

### Entities to Build

**Report**
| Field | Type | Notes |
|--------------|---------------------|----------------------------------------------------|
| id | UUID | |
| citizenId | UUID | FK to User |
| imageUrl | String | Cloudinary URL |
| description | String | Max 1000 chars |
| category | Category (enum) | POTHOLE, STREETLIGHT, GARBAGE, WATER_LEAK, ROAD_DAMAGE |
| location | Point | PostGIS geometry — lat/lng |
| status | ReportStatus (enum) | CREATED → IN_PROGRESS → COMPLETED |
| supportCount | int | Incremented on duplicate detection |
| createdAt | Instant | |

### APIs to Build

| Method | Endpoint              | Description                                                     |
| ------ | --------------------- | --------------------------------------------------------------- |
| POST   | /reports              | Create report — multipart/form-data (image + fields)            |
| GET    | /reports/nearby       | Reports within radius. Params: lat, lng, radius (default 5000m) |
| GET    | /reports/{id}         | Single report by ID                                             |
| POST   | /reports/{id}/support | Increment support count (one per user per report)               |

### PostGIS Geo Query

```sql
SELECT * FROM reports
WHERE ST_DWithin(
  location::geography,
  ST_MakePoint(:lng, :lat)::geography,
  :radiusMetres
)
ORDER BY ST_Distance(location::geography, ST_MakePoint(:lng, :lat)::geography);
```

⚠️ Always use `::geography` cast for accurate metre-based distance.

### Duplicate Detection Logic

- On `POST /reports`: run geo query within **50 metres**, same category
- If match found → call `POST /reports/{existingId}/support` instead of creating
- Return HTTP 200 with existing report + message: "Existing report found — your support has been added"

### Phase 3 Definition of Done

- [ ] POST /reports creates report, uploads image, stores PostGIS point
- [ ] GET /reports/nearby returns correct results with varying radius
- [ ] Support count increments correctly — duplicate prevention works
- [ ] Geo query runs in < 200ms against 1,000 seeded reports
- [x] Test coverage > 65%

---

# Known Issues & Fixes Applied

## ✅ Fix 1 — HTTP 403 Instead of 401 on Invalid/Missing JWT

**Root cause:**
`JwtAuthFilter` was calling `filterChain.doFilter()` even when `validateToken()`
returned false. Spring's `ExceptionTranslationFilter` saw an empty `SecurityContext`
and called `AccessDeniedHandler` (403) instead of `AuthenticationEntryPoint` (401).

**Fix applied in `JwtAuthFilter`:**

- Invalid/expired token with `Bearer` header → write 401 directly + `return` (stop chain)
- Missing `Authorization` header → pass through (Spring fires `CustomAuthenticationEntryPoint` → 401)
- Fixed wrong import: `io.jsonwebtoken.io.IOException` replaced with `java.io.IOException`

**Correct behaviour after fix:**
| Scenario | Status |
|---------------------------------|--------|
| Valid token → /user/me | 200 ✅ |
| Expired token → /user/me | 401 ✅ |
| Missing token → /user/me | 401 ✅ |
| Malformed token → /user/me | 401 ✅ |
| Valid token, wrong role | 403 ✅ |

---

## ✅ Fix 2 — /logout Returning 500

**Root causes:**

1. `IllegalStateException` thrown inside `@Transactional` was being wrapped by Spring
   before reaching `GlobalExceptionHandler` → raw 500. Fixed by throwing
   `IllegalArgumentException` (maps cleanly to 400).
2. `refreshTokenRepository.findByUser(user).forEach(t -> t.setRevoked(true))` in
   `login()` mutated entities without calling `saveAll()` → old tokens stayed active.

**Fix applied:**

- `logout()` now throws `IllegalArgumentException` (not `IllegalStateException`)
- `login()` now calls `refreshTokenRepository.saveAll(existingTokens)` after revoking

---

## ✅ Fix 3 — application.properties Using YAML Syntax

**Root cause:**
`application.properties` was using YAML-style nested syntax:

```
jwt:
  secret: your-key   ← INVALID in .properties files
```

In Docker, strict parsing caused `jwt.secret` to resolve as `null` →
`Keys.hmacShaKeyFor(null)` throws → all JWT operations fail → 403/500.

In the IDE this was silently ignored, which is why it worked in the debugger
but failed in Docker.

**Fix applied:** Converted all properties to flat dot-notation:

```properties
jwt.secret=your-very-secret-key-that-is-at-least-32-bytes-long
jwt.expiration=900000
```

---

## ⚠️ Fix 4 — Docker: Backend Starts Before DB is Ready (Partially Resolved)

**Root cause:**
`depends_on: postgres` only waits for the container to _start_, not for PostgreSQL
to _accept connections_. Backend crashed on startup because DB wasn't ready.

**Fix applied in `docker-compose.yml`:**

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 10s
    timeout: 5s
    retries: 5

backend:
  depends_on:
    postgres:
      condition: service_healthy

## ✅ Fix 5 — Access Token Still Valid After Logout

**Root cause:**
JWT access tokens are stateless — revoking the refresh token only blocks
new access tokens from being issued. The current access token remained
valid until its 15-minute TTL expired.

**Fix applied — In-Memory Token Blacklist:**
- `TokenBlacklistService`: ConcurrentHashMap of token → expiry
- On logout, access token extracted from `Authorization` header and
  blacklisted for its remaining lifetime
- `JwtAuthFilter` checks blacklist before setting authentication
- `TokenBlacklistCleanupTask` purges expired entries every 15 minutes
  via @Scheduled to prevent memory leak

**Known limitation:**
Blacklist resets on server restart. Tokens revoked before restart
become valid again until their natural expiry. Acceptable for Phase 2.
Migrate to Redis blacklist in Release 4 when Redis is added.

---

## ✅ Fix 6 — Spring Boot 3.4+ Test Mocking Package Change

**Root cause:**
The `@MockBean` annotation was historically available in `org.springframework.boot.test.mock.mockito.MockBean`.
However, starting from Spring Boot 3.4.0 (Spring Framework 6.2), this package and annotation were deprecated/removed, which caused compilation failures during the implementation of the automated test suite.

**Fix applied:**
- Migrated all usages of `@MockBean` to `@MockitoBean` located at `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- This ensures test dependencies compile and run correctly on newer Spring Boot environments.

---

## ✅ Fix 7 — Test DTO Property Name Mismatches

**Root cause:**
During the creation of the generic `TestDataFactory`, property assignments to DTOs were modeled incorrectly (e.g., calling `setFirstName` and `setLastName` on `RegisterRequest`, and `setFirstName` on `ProfileUpdateDto`). These did not match the actual implementations.
`RegisterRequest` uses `name` instead of separated fields and requires a `Role`, and `ProfileUpdateDto` uses `name`.

**Fix applied:**
- Updated the testing factory to properly map the correct variable fields (`setName("Test User")` and `setRole(Role.CITIZEN)`).

---

## ⚠️ Fix 8 — Windows Docker Desktop Testcontainers Failure

**Root cause:**
When executing `mvnw clean verify`, `Testcontainers` (v1.19.7) failed to discover the Docker Daemon, throwing: `IllegalStateException: Could not find a valid Docker environment`. This typically occurs on Windows systems when the default Docker Desktop context `desktop-linux` pipe isn't mapped automatically, or if TLS-less TCP exposes aren't configured.

**Fix attempted / Workaround:**
- Code implementation is correct and valid. Tests must run inside a properly configured Windows Docker environment.
- **Solution:** Ensure Docker Desktop is active, and under settings "Expose daemon on tcp://localhost:2375 without TLS" is toggled ON. Alternatively, specify the `DOCKER_HOST` environment variable pointing to the appropriate pipe (e.g., `npipe:////./pipe/dockerDesktopLinuxEngine`) prior to executing Maven.

# Development Guidelines

## General Rules

- Keep controllers thin — no business logic
- Use DTOs always — never expose entities directly in responses
- Business logic belongs in services only
- Repository = persistence only, no logic
- Use `@Transactional` on all service methods that write to DB
- Follow modular boundaries — modules must not reach into each other's repositories

## Exception Handling Rules

- `IllegalArgumentException` → 400 Bad Request
- `IllegalStateException` → 409 Conflict
- `UsernameNotFoundException` → handled by Spring Security → 401
- Never throw raw `RuntimeException` from service methods — always use typed exceptions
- `GlobalExceptionHandler` must catch both `IllegalArgumentException` and
  `IllegalStateException` explicitly to prevent 500s from `@Transactional` wrapping

## Security Rules

- Always use `hasRole('X')` with `@PreAuthorize` — never `hasAuthority('X')`
- Never call `filterChain.doFilter()` after writing an error response in a filter
- `JwtAuthFilter` must `return` immediately after writing 401 — do not continue the chain
- Public endpoints (`/auth/**`) must be explicitly listed in `SecurityConfig.permitAll()`

## Docker Rules

- Never use `localhost` in `SPRING_DATASOURCE_URL` inside Docker — use the service name (e.g. `postgres`)
- Always use flat dot-notation in `application.properties` — never YAML-style nesting
- Always pass `JWT_SECRET` and `JWT_EXPIRATION` as env vars in `docker-compose.yml`
- Use `condition: service_healthy` on `depends_on` — never bare `depends_on`
- Run `docker-compose logs backend` first when debugging startup failures

---

# Folder Structure

```
src/main/java/com/snapfix/
├── config/
│   └── SecurityConfig.java
├── common/
│   ├── entity/       → BaseEntity, Location
│   ├── exception/    → GlobalExceptionHandler
│   ├── dto/          → ApiResponse
│   └── util/         → JwtUtil
├── auth/
│   ├── controller/   → AuthController
│   ├── service/      → AuthService
│   ├── repository/   → RefreshTokenRepository
│   ├── entity/       → RefreshToken
│   ├── dto/          → RegisterRequest, LoginRequest, LogoutRequest, AuthResponse
│   └── security/     → JwtAuthFilter, CustomUserDetailsService,
│                        CustomAuthenticationEntryPoint, CustomAccessDeniedHandler
├── user/
│   ├── controller/   → UserController
│   ├── service/      → UserService
│   ├── repository/   → UserRepository, CitizenProfileRepository, WorkerProfileRepository
│   ├── entity/       → User, CitizenProfile, WorkerProfile, Role
│   └── dto/          → UserResponse, ProfileUpdateDto
├── report/           → (Phase 3)
├── notification/     → (Phase 4)
├── storage/          → StorageService (Cloudinary)
└── geo/              → (Phase 3)
```

---

# Docker Environment

```yaml
# docker-compose.yml runs:
postgres:   postgis/postgis:15-3.3  → port 5432
backend:    build: .                → port 8080
```

**Required env vars for backend container:**

```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/snapfix
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-very-secret-key-that-is-at-least-32-bytes-long
JWT_EXPIRATION=900000
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

---

# Full Roadmap

## Release 1 – Civic Reporting Foundation

| Phase | Description               | Status     |
| ----- | ------------------------- | ---------- |
| 1     | Platform Foundation       | ✅ Done    |
| 2     | Identity & User System    | ✅ Done    |
| 3     | Civic Report System       | 🚧 Next    |
| 4     | Discovery & Notifications | ⬜ Pending |

**Release 1 Final Checklist:**

- [ ] Auth flow: register → login → JWT → protected endpoint
- [ ] Report creation: image → Cloudinary, GPS → PostGIS, fields → DB
- [ ] Nearby search: reports returned sorted by distance
- [ ] Support: count increments, duplicate detection in place
- [ ] Notifications: stored on report creation, retrievable
- [ ] All containers start cleanly with docker-compose up
- [ ] Test coverage > 70%

## Release 2 – Worker Marketplace & Task Assignment

- Worker profile + location tracking
- Bidding marketplace (place, withdraw, approve bids)
- Admin governance + AdminActionLog
- Task lifecycle engine (ASSIGNED → IN_PROGRESS)

## Release 3 – Task Completion, Verification & Payment

- Proof of work system (image upload + GPS)
- Citizen verification workflow (VERIFIED / REJECTED)
- Retry logic (max 3 retries)
- Payment + wallet system
- Worker reputation / rating

## Release 4 – AI Intelligence & Event-Driven Architecture

- Kafka event bus replacing synchronous service calls
- FastAPI AI service: image validation, category classification, duplicate detection
- pgvector for embedding-based similarity search
- Priority scoring engine
- Real-time WebSocket notifications

## Release 5 – Production Hardening & Scalability

- Spring Cloud Gateway (JWT at gateway level)
- Redis rate limiting + caching
- Observability: Prometheus + Grafana + ELK stack
- GitHub Actions CI/CD
- Kubernetes with HPA
- k6 load testing (target: p95 < 300ms at 1,000 concurrent users)
- City analytics dashboard

---

# Non-Functional Targets

| Metric              | Target                   |
| ------------------- | ------------------------ |
| Reports supported   | 1,000                    |
| Concurrent users    | 100                      |
| API response time   | < 200ms                  |
| Geo query (Phase 3) | < 200ms on 1,000 reports |
