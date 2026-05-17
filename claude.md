# SnapFix - AI Assistant Context

This file reflects the current backend state after Release 2 completion.

## Current Development Stage

| Field | Value |
| --- | --- |
| Product | SnapFix Backend |
| Architecture | Modular monolith |
| Current release | Release 2 - Worker Marketplace and Task Assignment |
| Status | Release 1 and Release 2 backend scope complete |
| Latest verification | `.\mvnw.cmd test` |
| Latest result | 33 tests, 0 failures, 0 errors |

## Modules

```text
auth
user
report
notification
worker
bid
admin
task
storage
geo
common
config
```

Future modules:

```text
proof / verification / payment / wallet / rating / event / ai / analytics
```

## Rules To Preserve

- Controllers stay thin.
- Business logic belongs in services.
- Repositories stay persistence-only.
- DTOs must not expose entities directly.
- Use `@PreAuthorize("hasRole('X')")`, not `hasAuthority`.
- Use `IllegalArgumentException` for 400 and `IllegalStateException` for 409.
- For PostGIS/JTS points, longitude is `x` and latitude is `y`.
- `GeoUtil.createPoint(lat, lng)` must store `Coordinate(lng, lat)`.

## Implemented API Surface

### Auth

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| POST | `/auth/refresh` | Public |
| POST | `/auth/logout` | Bearer token |

### User

| Method | Endpoint | Auth |
| --- | --- | --- |
| GET | `/user/me` | Authenticated |
| PUT | `/user/profile` | Authenticated |

### Reports

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/reports` | CITIZEN |
| GET | `/reports/nearby?lat=&lng=&radius=` | Authenticated |
| GET | `/reports/{id}` | Authenticated |
| POST | `/reports/{id}/support` | CITIZEN |

### Workers

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/workers/profile` | WORKER |
| PUT | `/workers/profile` | WORKER |
| GET | `/workers/profile` | WORKER |
| POST | `/workers/location` | WORKER |
| GET | `/workers/reports/nearby` | WORKER |
| GET | `/workers/tasks` | WORKER |

### Bids

| Method | Endpoint | Auth |
| --- | --- | --- |
| POST | `/bids` | WORKER |
| DELETE | `/bids/{bidId}` | Owner WORKER |
| GET | `/bids/my` | WORKER |

### Admin

| Method | Endpoint | Auth |
| --- | --- | --- |
| GET | `/admin/reports` | ADMIN |
| GET | `/admin/reports/{id}/bids` | ADMIN |
| POST | `/admin/bids/{bidId}/approve` | ADMIN |
| POST | `/admin/bids/{bidId}/reject` | ADMIN |

### Tasks

| Method | Endpoint | Auth |
| --- | --- | --- |
| GET | `/tasks/{id}` | Assigned WORKER |
| PATCH | `/tasks/{id}/start` | Assigned WORKER |

### Notifications

| Method | Endpoint | Auth |
| --- | --- | --- |
| GET | `/notifications?unread=true|false` | Authenticated |
| PATCH | `/notifications/{id}/read` | Owner |

## Release 2 Data Model Notes

### WorkerProfile

- Shared primary key with `User`.
- `skills` uses `@ElementCollection`.
- `currentLocation` is PostGIS `geometry(Point, 4326)`.
- `available` controls whether the worker may bid.
- `rating`, `completedTasks`, and `walletBalance` are Release 3-ready fields.

### Bid

- One bid per `(report_id, worker_id)`.
- Statuses: `ACTIVE`, `WITHDRAWN`, `APPROVED`, `REJECTED`.
- Workers may bid only on reports in `CREATED` status.
- Workers may withdraw only their own active bids.

### Task

- One task per report through unique `report_id`.
- Status starts as `ASSIGNED`.
- Worker can move own task to `IN_PROGRESS`.
- `retryCount` defaults to 0 for Release 3 retry logic.

### AdminActionLog

- Written for admin approve/reject decisions.
- Stores admin user, action text, target id, note and timestamp.

## Release 2 Approval Flow

```text
ADMIN -> POST /admin/bids/{bidId}/approve
  -> AdminService.approveBid
  -> BidService.approveBid in one transaction
     -> selected bid ACTIVE -> APPROVED
     -> competing active bids -> REJECTED
     -> create Task(status = ASSIGNED)
     -> report CREATED -> IN_PROGRESS
  -> AdminActionLog written
```

If task creation fails, the approval transaction must roll back.

## Test Coverage

Latest command:

```powershell
.\mvnw.cmd test
```

Latest result:

```text
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Release 2 tests:

```text
src/test/java/com/snapfix/integration/release2/Release2IntegrationTest.java
```

Coverage includes:

- Worker profile completion and PostGIS location.
- Worker nearby report discovery from stored location.
- Bid placement, duplicate rejection, optional resource note and withdrawal.
- Admin approval, competing bid rejection, task creation, report status update and admin log.
- Task ownership and `ASSIGNED -> IN_PROGRESS`.

## Release 2 Bugs and Fixes

| Bug | Cause | Fix |
| --- | --- | --- |
| Admin approve/reject called state-changing service twice | Admin orchestration repeated bid transition | Call once and log returned bid |
| Worker task access leaked by UUID | Role check without owner check | `TaskService` validates assigned worker |
| `TaskResponse` exposed entities | DTO contained `Report` and `User` | Response now contains UUIDs |
| Worker location used embedded lat/lng | Release 2 requires PostGIS location | Migrated to `Point` |
| Worker discovery used request coordinates | Reference says stored worker location | Endpoint now uses `WorkerProfile.currentLocation` |
| `resourceNote` optional field rejected valid input | Request used `Optional<String>` and bad validation | Normal string, nullable, defaults to empty |
| Worker profile creation conflicted with registration | Registration creates basic worker profile | `POST /workers/profile` completes existing profile when location is missing |
| Task route used `/task` | Reference uses `/tasks` | Route changed to `/tasks` |
| Task retry default was 3 | Retry count should start at 0 | Default changed to 0 |
| Report response lat/lng reversed in one constructor | JTS x/y confusion | Map `lat = y`, `lng = x` |

## Known Limitations

- No Flyway/Liquibase yet.
- Access-token blacklist is in-memory.
- Report location should get explicit GiST index.
- Report ownership uses UUID field instead of full FK relationship.
- Real-time notification push is deferred.
- Release 3 proof, verification, retry, payment, wallet and rating flows are not implemented.

## Next Release

Release 3 should implement:

- Proof upload for tasks in `IN_PROGRESS`.
- Citizen verification/rejection.
- Retry flow with max 3 retries.
- Admin final approval.
- Payment and wallet transactions.
- Worker rating updates.
