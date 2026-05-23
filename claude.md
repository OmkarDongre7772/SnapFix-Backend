# SnapFix - AI Assistant Context

This file reflects the current backend state after Release 3 Phase 2 completion.

## Current Development Stage

| Field | Value |
| --- | --- |
| Product | SnapFix Backend |
| Architecture | Modular monolith |
| Current release | Release 3 - Completion and Verification |
| Status | Release 1, Release 2, and Release 3 Phase 1-2 backend scope complete |
| Latest verification | `.\mvnw.cmd test` |
| Latest result | 44 tests, 0 failures, 0 errors |

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
proof
verification
storage
geo
common
config
```

Future modules:

```text
payment / wallet / rating / event / ai / analytics
```

## Rules To Preserve

- Controllers stay thin.
- Business logic belongs in services.
- Repositories stay persistence-only.
- DTOs must not expose entities directly.
- Use `@PreAuthorize("hasRole('X')")`, not `hasAuthority`.
- Use `IllegalArgumentException` for 400 and `IllegalStateException` for 409.
- Missing multipart request parts should return 400.
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
| POST | `/tasks/{taskId}/proof` | Assigned WORKER |
| GET | `/tasks/{taskId}/proof` | Assigned WORKER, report CITIZEN or ADMIN |
| POST | `/tasks/{taskId}/verify?status=VERIFIED|REJECTED` | Report CITIZEN |

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

### Proof

- One proof per task through unique `task_id`.
- Assigned worker uploads proof only when task is `IN_PROGRESS`.
- Proof stores image URL, PostGIS GPS point, remarks, worker and task.
- Proof upload moves task to `PROOF_SUBMITTED`.
- Proof viewing is limited to assigned worker, report citizen and admin.

### Verification

- One verification per task through unique `task_id`.
- Report citizen can verify or reject only after proof is submitted.
- `VERIFIED` moves task to `VERIFIED_BY_CITIZEN`.
- `REJECTED` moves task to `REJECTED` and increments retry count.
- Rejection is blocked once retry count reaches 3.
- Verification stores task id, citizen id, status, comments and timestamp.

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

## Release 3 Verification Flow

```text
WORKER -> PATCH /tasks/{taskId}/start
  -> task ASSIGNED -> IN_PROGRESS

WORKER -> POST /tasks/{taskId}/proof
  -> validate assigned worker
  -> validate task IN_PROGRESS
  -> upload image through StorageService
  -> save proof image URL, GPS point and remarks
  -> task IN_PROGRESS -> PROOF_SUBMITTED

CITIZEN -> POST /tasks/{taskId}/verify?status=VERIFIED|REJECTED
  -> validate report citizen owns task report
  -> validate task PROOF_SUBMITTED
  -> VERIFIED -> task VERIFIED_BY_CITIZEN
  -> REJECTED -> task REJECTED and retryCount + 1
```

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

Release tests:

```text
src/test/java/com/snapfix/integration/release2/Release2IntegrationTest.java
src/test/java/com/snapfix/integration/release3/Release3Phase1ProofIntegrationTest.java
src/test/java/com/snapfix/integration/release3/Release3Phase2VerificationIntegrationTest.java
```

Coverage includes:

- Worker profile completion and PostGIS location.
- Worker nearby report discovery from stored location.
- Bid placement, duplicate rejection, optional resource note and withdrawal.
- Admin approval, competing bid rejection, task creation, report status update and admin log.
- Task ownership and `ASSIGNED -> IN_PROGRESS`.
- Proof upload, proof ownership and `IN_PROGRESS -> PROOF_SUBMITTED`.
- Citizen verification, rejection, retry count and max retry protection.

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

## Release 3 Phase 1 and 2 Bugs and Fixes

| Bug | Cause | Fix |
| --- | --- | --- |
| Missing proof image returned 500 | Missing multipart exception was handled generically | Map missing request parts to 400 |
| Proof access leaked by task id | Role check without task ownership check | Allow assigned worker, report citizen or admin only |
| Citizen verification failed ownership checks | Used worker-only task lookup | Added neutral task lookup for verification |
| Valid verification failed after status mutation | Checked source state after changing task status | Validate `PROOF_SUBMITTED` before mutation |
| Verification saved task id as citizen id | Wrong UUID assigned | Store report owner citizen id |
| Rejection with comments verified task | Branch used comments to decide status | Branch only on requested verification status |
| Rejections had no upper bound | Retry rule was not enforced | Block rejection when `retryCount >= 3` |

## Known Limitations

- No Flyway/Liquibase yet.
- Access-token blacklist is in-memory.
- Report location should get explicit GiST index.
- Report ownership uses UUID field instead of full FK relationship.
- Real-time notification push is deferred.
- Release 3 payment, wallet and rating flows are not implemented.
- Auto-verification scheduling exists but needs production policy review.

## Next Release

Release 3 remaining work:

- Admin final approval.
- Payment and wallet transactions.
- Worker rating updates.
