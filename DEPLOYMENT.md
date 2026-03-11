# Deployment Guide

This backend is deployed as a standard Spring Boot jar and does not require
Docker. Firebase services are external, so deployment mainly consists of placing
the jar, environment configuration, and secret file correctly on the target
machine.

## 1. Prerequisites

- Java 21 installed on the server
- Access to the Firebase project used by the backend
- Firebase service account JSON file available on the server
- Network access to Firebase endpoints

## 2. Files Needed on the Server

Copy or provision:

- built jar from `target/BusBuddy_Backend-0.0.1-SNAPSHOT.jar`
- `.env` or equivalent environment configuration
- Firebase service account JSON file

Recommended secret location on the server:

```text
/path/to/secrets/busbuddy-729d4-8a30dc491cc5.json
```

## 3. Environment Variables

Required:

```env
APP_SECURITY_JWT_SECRET=<strong-secret>
FIREBASE_DATABASE_URL=https://busbuddy-729d4-default-rtdb.firebaseio.com/
FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/secrets/busbuddy-729d4-8a30dc491cc5.json
```

Optional:

```env
APP_SECURITY_JWT_EXPIRATION_MS=604800000
APP_TIMEZONE=Asia/Kolkata
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

## 4. Build Before Deployment

On a build machine:

```bat
mvnw.cmd clean compile
mvnw.cmd test
mvnw.cmd package
```

This produces the jar in `target/`.

## 5. Run on the Target Machine

Example:

```bat
java -jar BusBuddy_Backend-0.0.1-SNAPSHOT.jar
```

Or from the project folder:

```bat
java -jar target/BusBuddy_Backend-0.0.1-SNAPSHOT.jar
```

## 6. Post-Deployment Verification

Verify:

- health endpoint: `GET /actuator/health`
- OpenAPI docs: `GET /api-docs`
- Swagger UI: `GET /swagger-ui.html`
- public route/stop endpoints are reachable
- protected mutation endpoints deny unauthenticated access

## 7. Security Notes

Before public deployment:

- keep the Firebase service account file outside public web roots
- do not commit `.env` or secrets
- set production CORS origins
- review `SECURITY_CLASSIFICATION.md`
- confirm which endpoints are public vs protected

## 8. Logging and Monitoring

The application logs:

- Firebase initialization
- selected operational service actions
- security filter chain startup details

In production, run the app under a process manager or service wrapper that
captures stdout/stderr and retains logs.

## 9. Recommended Next Hardening Steps

If the backend will be exposed publicly to multiple operators or companies,
consider adding:

- role/claim-based authorization
- company ownership checks
- extra security integration tests
- safer generic 500 error messages for public clients
