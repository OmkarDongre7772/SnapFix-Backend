# SnapFix - AI Assistant Context

Use this file as the current project truth for coding agents. Older reference PDFs and generated
status notes may be stale; the code and this file reflect the Release 3 Phase 2 completion state.

## Project Overview

SnapFix is a civic infrastructure reporting platform. Citizens report issues such as potholes,
garbage, broken streetlights, water leaks and road damage. Workers discover nearby reports, place
bids, receive assigned tasks after admin approval, upload proof of work, and citizens verify or
reject completed work.

This repository contains the SnapFix Spring Boot backend.

## Current Development Stage

| Field | Value |
| --- | --- |
| Current release | Release 3 - Completion and Verification |
| Release 1 | Complete |
| Release 2 | Complete |
| Release 3 Phase 1 | Complete |
| Release 3 Phase 2 | Complete |
| Latest verification | `.\mvnw.cmd test` |
| Latest result | 44 tests, 0 failures, 0 errors |

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.6, Spring Security, Spring Data JPA |
| Database | PostgreSQL + PostGIS |
| Spatial | Hibernate Spatial + JTS |
| Storage | Cloudinary |
| Testing | JUnit 5, MockitoBean, AssertJ, Testcontainers |
| DevOps | Docker, Docker Compose |

## Architecture

Modular monolith modules:

```text
auth
user
report
notification
worker
bid
admin
task
proof
verification
storage
geo
common
config
```

Future modules:

```text
payment
wallet
rating
event
ai
analytics
```

Module pattern:

```text
controller -> API layer, thin
service    -> business logic and transactions
repository -> persistence only
entity     -> database model
dto        -> request/response model, never expose entities directly
```

## Security Rules

- Use `@PreAuthorize("hasRole('CITIZEN')")`, `hasRole('WORKER')`, or `hasRole('ADMIN')`.
- Do not use `hasAuthority` for role checks.
- Missing/invalid bearer tokens should return 401.
- Valid token with wrong role should return 403.
- After a filter writes an error response, return immediately and do not continue the chain.

## Exception Rules

- `IllegalArgumentException` maps to 400.
- `IllegalStateException` maps to 409.
- Access-denied exceptions map to 403.
- Missing multipart request parts map to 400.
- Do not throw raw `RuntimeException` from services.

## Geo Rules

- JTS `Point.getX()` is longitude.
- JTS `Point.getY()` is latitude.
- `GeoUtil.createPoint(lat, lng)` stores `Coordinate(lng, lat)`.
- Radius queries should cast to `::geography` for metre-based distance.

## Implemented Release 1 Scope

- Auth/register/login/refresh/logout.
- Refresh token rotation and access-token blacklist on logout.
- User profile APIs.
- Multipart report creation.
- Cloudinary image upload through `StorageService`.
- Report location stored as PostGIS point.
- Nearby reports sorted by distance.
- Duplicate detection within 50 metres.
- Support count and one-support-per-user rule.
- Stored notifications and mark-as-read ownership protection.
- Worker-only report discovery.

## Implemented Release 2 Scope

### Worker System

- `POST /workers/profile` completes worker profile after registration.
- `PUT /workers/profile` updates skills, location and availability.
- `GET /workers/profile` returns worker profile with lat/lng DTO fields.
- `POST /workers/location` updates current PostGIS location.
- `GET /workers/reports/nearby` uses stored worker location, not query coordinates.
- Nearby discovery expands from 5 km to 10 km when fewer than 5 reports are found.

### Bidding Marketplace

- `POST /bids` places a bid on a `CREATED` report.
- `DELETE /bids/{bidId}` withdraws only the current worker's active bid.
- `GET /bids/my` lists the current worker's bids.
- `(report_id, worker_id)` unique constraint prevents duplicate bids.
- `resourceNote` is optional and defaults to empty string.

### Admin Governance

- `GET /admin/reports` lists paginated reports with optional status filter.
- `GET /admin/reports/{id}/bids` lists bids for a report.
- `POST /admin/bids/{bidId}/approve` approves one active bid.
- `POST /admin/bids/{bidId}/reject` rejects one active bid.
- Admin decisions write `AdminActionLog`.

### Task Lifecycle

- Bid approval creates one task per report.
- `GET /workers/tasks` lists tasks assigned to current worker.
- `GET /tasks/{id}` allows only the assigned worker to fetch task detail.
- `PATCH /tasks/{id}/start` moves assigned task from `ASSIGNED` to `IN_PROGRESS`.

## Implemented Release 3 Scope

### Proof of Work

- `POST /tasks/{taskId}/proof` allows only the assigned worker to upload proof.
- Proof upload requires task status `IN_PROGRESS`.
- Proof stores Cloudinary image URL, PostGIS GPS point, remarks, task and worker.
- Successful proof upload moves task from `IN_PROGRESS` to `PROOF_SUBMITTED`.
- One proof per task is enforced.
- `GET /tasks/{taskId}/proof` allows only assigned worker, report citizen or admin.

### Citizen Verification and Retry

- `POST /tasks/{taskId}/verify?status=VERIFIED` lets the report citizen accept proof.
- Verified tasks move to `VERIFIED_BY_CITIZEN`.
- `POST /tasks/{taskId}/verify?status=REJECTED` lets the report citizen reject proof.
- Rejected tasks move to `REJECTED`.
- Rejection increments `Task.retryCount`.
- Rejection is blocked once `retryCount >= 3`.
- Verification records store task id, citizen id, status, comments and timestamp.

## Release 2 Final Checklist

- [x] Worker profile and PostGIS location tracking working
- [x] Worker can place and withdraw bids
- [x] Duplicate bid returns conflict
- [x] Admin can view and approve bids
- [x] Bid approval rejects competing bids and creates task
- [x] Worker can view task and mark it `IN_PROGRESS`
- [x] AdminActionLog records admin decisions
- [x] Release 2 integration tests added

## Release 3 Phase 1 and 2 Checklist

- [x] Worker can upload proof for an in-progress assigned task
- [x] Proof upload persists image URL, GPS point, remarks, worker and task
- [x] Task moves to `PROOF_SUBMITTED` after proof upload
- [x] Proof viewing is limited to assigned worker, report citizen and admin
- [x] Report citizen can verify submitted proof
- [x] Report citizen can reject submitted proof
- [x] Rejection increments retry count
- [x] Max retry protection blocks rejection at 3 retries
- [x] Release 3 Phase 1 integration tests added
- [x] Release 3 Phase 2 integration tests added

## Current APIs

### Auth

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| POST | `/auth/refresh` | Public |
| POST | `/auth/logout` | Bearer |

### Reports

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/reports` | CITIZEN |
| GET | `/reports/nearby?lat=&lng=&radius=` | Authenticated |
| GET | `/reports/{id}` | Authenticated |
| POST | `/reports/{id}/support` | CITIZEN |

### Workers, Bids, Admin, Tasks

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/workers/profile` | WORKER |
| PUT | `/workers/profile` | WORKER |
| GET | `/workers/profile` | WORKER |
| POST | `/workers/location` | WORKER |
| GET | `/workers/reports/nearby` | WORKER |
| GET | `/workers/tasks` | WORKER |
| POST | `/bids` | WORKER |
| DELETE | `/bids/{bidId}` | Owner WORKER |
| GET | `/bids/my` | WORKER |
| GET | `/admin/reports` | ADMIN |
| GET | `/admin/reports/{id}/bids` | ADMIN |
| POST | `/admin/bids/{bidId}/approve` | ADMIN |
| POST | `/admin/bids/{bidId}/reject` | ADMIN |
| GET | `/tasks/{id}` | Assigned WORKER |
| PATCH | `/tasks/{id}/start` | Assigned WORKER |
| POST | `/tasks/{taskId}/proof` | Assigned WORKER |
| GET | `/tasks/{taskId}/proof` | Assigned WORKER, report CITIZEN or ADMIN |
| POST | `/tasks/{taskId}/verify?status=VERIFIED|REJECTED` | Report CITIZEN |

## Release 2 Bugs and Fixes

1. Admin approval/rejection double-called bid transitions.
   - Fixed by calling bid service once and logging the returned bid.

2. Worker task endpoints had role checks but no ownership checks.
   - Fixed by validating `task.worker.id` against the authenticated worker id.

3. `TaskResponse` exposed `Report` and `User` entities.
   - Fixed by returning `reportId` and `workerId` UUIDs.

4. Worker location was embedded lat/lng.
   - Fixed by storing `WorkerProfile.currentLocation` as PostGIS `Point`.

5. Worker discovery accepted arbitrary lat/lng query params.
   - Fixed by using the current worker's stored location.

6. Registration-created worker profiles conflicted with `POST /workers/profile`.
   - Fixed by allowing profile completion when current location is still missing.

7. `resourceNote` optional handling was broken.
   - Fixed by using nullable/default string instead of `Optional<String>` in DTO.

8. Task route used singular `/task`.
   - Fixed to `/tasks`.

9. `Task.retryCount` started at 3.
   - Fixed to default 0.

10. Report response lat/lng were reversed in a constructor.
    - Fixed with `lat = point.getY()` and `lng = point.getX()`.

## Release 3 Phase 1 and 2 Bugs and Fixes

1. Missing proof image returned a generic 500.
   - Fixed by mapping missing multipart request parts to 400.

2. Proof view endpoint had role checks but no ownership checks.
   - Fixed by allowing only assigned worker, report citizen or admin.

3. Verification used worker-only task lookup.
   - Fixed by using neutral task lookup for citizen-owned verification.

4. Verification changed task status before checking `PROOF_SUBMITTED`.
   - Fixed by validating source state before mutating task status.

5. Verification stored task id as citizen id.
   - Fixed by storing the report owner citizen id.

6. Rejection with comments could incorrectly verify a task.
   - Fixed by branching explicitly on `VERIFIED` and `REJECTED`.

7. Rejections had no retry limit.
   - Fixed by blocking rejection when retry count reaches 3.

## Test Coverage

Latest command:

```powershell
.\mvnw.cmd test
```

Latest result:

```text
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Release test files:

```text
src/test/java/com/snapfix/integration/release2/Release2IntegrationTest.java
src/test/java/com/snapfix/integration/release3/Release3Phase1ProofIntegrationTest.java
src/test/java/com/snapfix/integration/release3/Release3Phase2VerificationIntegrationTest.java
```

## Known Limitations

- No Flyway/Liquibase yet.
- In-memory access-token blacklist should move to Redis later.
- Add explicit GiST index for `reports.location` before production data volumes.
- Real-time notification delivery is deferred.
- Release 3 payment, wallet and rating flows are not implemented.
- Auto-verification scheduling exists as a foundation but needs final production policy review.

## Roadmap

| Release | Focus | Status |
| --- | --- | --- |
| Release 1 | Civic Reporting Foundation | Complete |
| Release 2 | Worker Marketplace and Task Assignment | Complete |
| Release 3 | Completion, Verification and Payment | Phase 1 and 2 Complete |
| Release 4 | AI Intelligence and Event-Driven Architecture | Planned |
| Release 5 | Production Hardening and Scalability | Planned |
