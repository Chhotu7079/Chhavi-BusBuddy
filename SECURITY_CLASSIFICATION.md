# Backend Endpoint Security Classification

This document defines the **business-level authorization model** for BusBuddy Backend.
It is the source of truth for who can access which endpoints and under what ownership conditions.

---

## Final Authorization Model

### 1) Public
No authentication required.

Used for read-only/public-consumer endpoints (tracking, discovery, docs, health).

### 2) OWNER/EMPLOYEE (own-resource-only)
Requires valid backend JWT and `OWNER` or `EMPLOYEE` role.

Rules:
- A `COMPANY` user can only mutate/read protected business resources that belong to that same company.
- Ownership checks are enforced in service layer using ownership resolution (`OwnershipService`) + authorization guard logic.
- If ownership does not match, request must be rejected.

### 3) ADMIN (full access)
Requires valid backend JWT and `ADMIN` role.

Rules:
- `ADMIN` can access all resources across companies.
- `ADMIN` bypasses company ownership restrictions where business policy allows global control.

---

## Ownership Decision Rules (Business Policy)

For protected resource operations:

1. Resolve owner company ID of target resource (route/bus/etc.).
2. If caller role is `ADMIN` -> allow.
3. If caller role is `COMPANY` -> allow only when `callerCompanyId == resourceOwnerCompanyId`.
4. Otherwise -> deny.

This applies to route management, bus management, and route-history mutation operations.

---

## Endpoint Classification Matrix

> Notes:
> - Endpoint paths below reflect current API surface used by this backend.
> - “COMPANY (own resource only)” means authenticated company users are constrained by ownership checks.

| Endpoint | Method | Purpose | Final Access |
|---|---:|---|---|
| `/auth/company/login` | POST | Exchange Firebase ID token for backend JWT | Public |
| `/verify-custom-token` | GET | Verify backend JWT | Public |
| `/companies/signup` | POST | Register company/operator account | Public |
| `/companies?email=` | GET | Resolve company by email | Public |
| `/routes/{routeId}` | GET | Get route details | Public |
| `/routes` | GET | List routes grouped by company | Public |
| `/stops/{stopId}` | GET | Get stop details | Public |
| `/stops?latitude=&longitude=&radius=` | GET | Nearby stops search | Public |
| `/stops/{stopId}/next-buses` | GET | Next buses from a stop | Public |
| `/buses/{busId}` | GET | Get bus with route details | Public |
| `/buses?code=` | GET | Get bus by code | Public |
| `/buses/{busId}/stops` | GET | Get stops for bus route | Public |
| `/buses/{busId}/arrivals?direction=` | GET | Upcoming arrivals by bus direction | Public |
| `/buses/{busId}/delays/average?direction=` | GET | Average delay analytics | Public |
| `/buses/{busId}/delays/current?direction=` | GET | Current delay estimate | Public |
| `/actuator/health` | GET | Health probe | Public |
| `/actuator/info` | GET | Service metadata | Public |
| `/api-docs/**` | GET | OpenAPI JSON endpoints | Public |
| `/swagger-ui.html` | GET | Swagger UI entry | Public |
| `/swagger-ui/**` | GET | Swagger static assets | Public |
| `/routes` | POST | Create route + associate/create stops | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/routes/{routeId}` | DELETE | Delete route | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/routes/{routeId}/delays/recompute` | POST | Recompute delays for one route | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/routes/delays/recompute` | POST | Recompute delays globally | ADMIN (full access) |
| `/routes/{routeId}/stops/reached` | POST | Mark stop reached in route history | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/routes/{routeId}/history/fix-gaps` | POST | Clean placeholder gaps in history | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/buses` | POST | Create bus | OWNER/EMPLOYEE (own resource only) / ADMIN |
| `/routes?busCode=` | GET | Candidate routes for a bus code | Public |
| `/buses/{busCode}/route` | PUT | Reassign bus to route | OWNER/EMPLOYEE (own resource only) / ADMIN |

---

## Service-Level Enforcement Mapping

The following business services are expected to enforce ownership semantics:

- **RouteService**
  - `addRoute`, `deleteRoute`, `updateDelay` enforce admin-or-owner behavior.

- **BusManagementService**
  - `addBus`, `getRoutesByBusCode`, `updateBusRoute` enforce admin-or-owner behavior.

- **BusHistoryService**
  - `updateStopReached`, `fixHistoryGaps` enforce admin-or-owner behavior.

---

## Error Handling Expectations for Authorization

Recommended status outcomes:

- Missing/invalid token -> `401 Unauthorized`
- Authenticated but insufficient role/ownership -> `403 Forbidden`
- Resource not found -> `404 Not Found`

Implementation note:
- Service-layer authorization denials should throw a *forbidden* error (mapped to HTTP 403), not an authentication error.

Public endpoints should avoid leaking internal details and should return controlled error payloads.

---

## Testing Requirements (Security & Authorization)

Minimum coverage should include:

1. **Public Access Tests**
   - Public endpoints accessible without JWT.

2. **Authentication Boundary Tests**
   - Protected endpoints reject missing/invalid JWT.

3. **Role Tests**
   - `ADMIN` allowed on admin/full-access operations.
   - `OWNER/EMPLOYEE` denied on admin-only operations.

4. **Ownership Tests**
   - `OWNER/EMPLOYEE` allowed on owned resources.
   - `OWNER/EMPLOYEE` denied on other company resources.

5. **Regression Tests**
   - Ensure new endpoints are classified and tested before release.

---

## Maintenance Rule

Whenever an endpoint is added or access behavior changes:

1. Update this document in the same change.
2. Add/adjust corresponding authorization tests.
3. Confirm alignment between controller policy and service-layer ownership checks.
