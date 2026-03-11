# BusBuddy Backend — Audit Checklist

This document is a **codebase audit checklist** based on a repository walk-through of `E:\Bus\BusBuddy\backend`.

It focuses on **things that look left out / inconsistent / risky**, with **severity** and **concrete fix steps**.

> Scope reviewed:
> - `src/main/java/**` (controllers, services, security, persistence, util, dto, exceptions)
> - `src/test/java/**`
> - root files (`pom.xml`, `application.properties`, docs, Postman collection)
> - presence of secrets/config files in workspace

---

## Severity scale

- **Critical**: security compromise / leaked secrets / privilege bypass likely
- **High**: production-impacting correctness issue, inconsistent API behavior, data integrity risk
- **Medium**: maintainability issue or edge-case bug risk
- **Low**: documentation, cleanup, minor inconsistencies

---

## Checklist

### 1) Secrets and sensitive files present in repo/workspace
**Severity: Critical**

**What I saw**
- `.env` contains a real `APP_SECURITY_JWT_SECRET`.
- `secrets/busbuddy-729d4-8a30dc491cc5.json` (Firebase service account) exists in `secrets/`.

**Why it matters**
- If these files were ever committed/pushed, they enable account takeover and backend token minting.

**Fix steps**
1. Ensure `.gitignore` excludes:
   - `.env`
   - `secrets/`
   - `*.json` service account credentials (or a specific filename)
   - `target/`
2. Rotate the JWT secret (generate new 32+ char secret) if it was ever shared.
3. Rotate/replace the Firebase service account key if it was ever pushed.
4. For local dev, use `.env.example` without secrets.

**Verification**
- Search history on remote or local VCS to confirm no secret was committed.
- App should still run when secrets are provided via environment variables.

---

### 2) Postman collection appears outdated vs current controllers
**Severity: Medium**

**What I saw**
- `BusBuddy.postman_collection.json` includes endpoints like:
  - `GET /route?id=...`
  - `GET /allRoutes`
  - `GET /stop?id=...`
- Current controllers expose:
  - `GET /routes/{id}`
  - `GET /routes`
  - `GET /stops/{id}`

**Why it matters**
- Dev/test workflows will fail or mislead new developers.

**Fix steps**
1. Update the Postman collection to match the current REST endpoints.
2. Ensure examples include correct auth headers for protected endpoints.
3. Add a short note in `README.md` that Swagger is the source of truth.

**Verification**
- Import collection into Postman and run the full checklist against a running backend.

---

### 3) 401 vs 403 mismatch between Spring Security and service-layer auth
**Severity: High**

**What I saw**
- `SecurityConfig` uses Spring Security authorization rules that will return **403** when access is denied.
- Service-layer `AuthorizationService` throws `UnauthorizedException` for ownership/role denials.
- `GlobalExceptionHandler` maps `UnauthorizedException` to **401**.

**Why it matters**
- Clients may receive **401 Unauthorized** when they are authenticated but forbidden.
- This also complicates debugging and violates typical HTTP semantics.

**Fix steps**
Choose one consistent approach:

Option A (recommended):
1. Replace `UnauthorizedException` with `ForbiddenException` (new exception) for authorization denials.
2. Map it to **403** in `GlobalExceptionHandler`.
3. Keep `UnauthorizedException` only for missing/invalid authentication.

Option B:
- Stop throwing auth exceptions from services and rely strictly on Spring Security method/endpoint authorization.

**Verification**
- Add/adjust integration tests:
  - missing token -> 401
  - valid token but wrong company -> 403
  - valid token admin -> 200

---

### 4) `RouteService.getRoute()` may not return expanded stops consistently
**Severity: High**

**What I saw**
- `getAllRoutesGroupedByCompany()` explicitly expands stop references via:
  - `route.buildStopOutboundReturn(document)`
- `getRoute(id)` returns `routeDocumentService.getRouteResource(id).getRoute()` without explicit stop expansion.

**Why it matters**
- API consumers may receive different shapes depending on which route endpoint they call.
- Could lead to missing stops in `GET /routes/{id}`.

**Fix steps**
1. Ensure `GET /routes/{id}` expands stops exactly like the list endpoint.
2. Consider centralizing route expansion logic (one helper used by both).

**Verification**
- Integration test asserts stops are present and consistent for both endpoints.

---

### 5) Firestore nested field access using dotted strings may be unreliable
**Severity: High**

**What I saw**
- `FirestoreDocumentMapper.getReferenceList(document, fieldName)` uses `document.get(fieldName)`.
- Call sites pass dotted paths like:
  - `"stops.forward"`
  - `"stops.back"`

**Why it matters**
- Firestore Java SDK nested access is safest via `FieldPath.of("stops","forward")`.
- If dotted access returns null, stop expansion and stop->route traversal will silently break.

**Fix steps**
1. Update `FirestoreDocumentMapper` to accept either:
   - `FieldPath` overloads, or
   - split dotted strings into `FieldPath.of(...)`.
2. Add tests for nested-field reference extraction.

**Verification**
- Confirm `Route.buildStopOutboundReturn()` returns non-empty stop lists for known seeded data.

---

### 6) GeoPoint deserializer is fragile (order-dependent token skipping)
**Severity: Medium**

**What I saw**
- `GeoPointDeserializer` advances tokens manually assuming a strict field order.

**Why it matters**
- Any JSON field reordering or additional fields can break deserialization.

**Fix steps**
1. Replace with a safer implementation:
   - parse as JSON object node
   - read `latitude` and `longitude` by field name
2. Add a small unit test for GeoPoint serialization/deserialization.

**Verification**
- POST/PUT payloads containing GeoPoint should deserialize reliably regardless of field order.

---

### 7) `target/` build output exists under repository root
**Severity: Low**

**What I saw**
- `target/` directory exists at the project root.

**Why it matters**
- Usually should be ignored in VCS and not part of the repo.

**Fix steps**
1. Add `target/` to `.gitignore` if not already.
2. Remove it from version control if tracked.

---

### 8) Minor: unused variables / misleading comments
**Severity: Low**

**Examples**
- `RouteService.getRoute()` declares `CollectionReference routes = ...` but doesn’t use it.
- Some comments claim expansion occurs where it may not.

**Fix steps**
1. Remove unused locals.
2. Update comments to match actual behavior.

---

## Suggested next actions (ordered)

1. **Critical hygiene**: remove/ignore secrets and rotate if ever committed.
2. Fix **401 vs 403** semantics and add regression tests.
3. Fix **stop expansion consistency** and Firestore nested field access.
4. Update Postman collection to match controllers.
5. Harden GeoPoint JSON handling.

---

## Notes
- This audit is based on static review of the codebase; runtime issues may depend on Firebase schema and actual stored document structure.
