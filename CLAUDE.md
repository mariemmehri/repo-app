# CLAUDE.md — repo-app

This file provides guidance to Claude Code when working inside `repo-app/`.
The parent `../CLAUDE.md` covers the full platform (infra, config, CI/CD overview).

## Project Structure

```
repo-app/
├── backend/          # Spring Boot 3.2.0 REST API — Java 17, Maven
├── frontend/         # Angular 17 SPA — served by Nginx
├── .github/workflows/
│   ├── ci.yml            # build+test+Trivy+push; develop → dev, main → staging (patches values-<env>.yaml in repo-config)
│   └── promote-prod.yml  # tag v*.*.* → crane copy (no rebuild) staging image → prod registry, patches values-prod.yaml
└── docker-compose.yml
```

**Branch → environment mapping** (see parent `../CLAUDE.md` for the full flow): push to `develop` deploys to namespace `dev`; push/merge to `main` deploys to `staging`; a `v*.*.*` tag on a `main` commit promotes the **already-built** image to `prod` (GitHub Environment `production` reviewer gate + manual ArgoCD sync). `promote-prod.yml` fails if the tagged commit was never built by CI — it never rebuilds.

## Backend

**Package root:** `com.example.hr`
**Port:** 8081

The backend has a real Postgres connection (Spring Data JPA + `org.postgresql:postgresql` driver pinned `42.7.2`, CVE-2024-1597) but **no business-domain entities/controllers** — there is still no employee/leave/payslip domain. `HealthController` serves `GET /api/health-check` (K8s readiness/liveness probe target), `GET /api/health` (explicit status), and `GET /api/db-health` (writes + counts a row via `HealthCheckRepository`/`HealthCheck` — the only JPA entity that exists, added purely to prove DB connectivity). `spring.datasource.*`/`spring.jpa.*` live in `application.properties`, driven by `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` env vars locally (defaults point at `docker-compose`'s `postgres` service) and overridden in staging by the `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD` env vars the Helm chart injects from the CNPG-generated secret `pg-staging-app` (see `repo-config`'s `charts/hr-app/templates/deployment-backend.yaml`).

## Frontend

**Framework:** Angular 17 (standalone components, no NgModule)
**Port:** 80 (Nginx), proxies `/api/*` → `http://hr-backend:8081`

Key files: `src/app/hr.service.ts` (HttpClient), `src/app/models.ts` (TS interfaces)

Note: the frontend still contains UI/service code calling employee/leave/payslip API routes that no longer exist on the backend (removed along with the persistence layer) — those calls will fail until the frontend is updated to match.

## Common Commands

### Local dev with Docker Compose
```bash
cd repo-app
docker compose up --build
# Frontend: http://localhost   Backend: http://localhost:8081/api/health
```

### Backend only
```bash
cd repo-app/backend
mvn spring-boot:run
# or compile + test:
mvn verify
```

### Frontend only
```bash
cd repo-app/frontend
npm install --legacy-peer-deps   # --legacy-peer-deps is REQUIRED
npm start                        # ng serve
npm run build                    # production build → dist/hr-frontend/browser/
```

## API Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/health` | `{"status":"UP"}` |
| GET | `/api/health-check` | K8s readiness/liveness probe target |
| GET | `/api/db-health` | `{"status":"UP","database":"postgresql","totalChecks":N}` (200) or `{"status":"DOWN",...}` (503) — real JPA insert+count round trip |

## Key Constraints

- `--legacy-peer-deps` is always required for `npm install` (Angular 17 peer-dep conflicts)
- Dockerfiles expect pre-built artifacts (JAR / dist/) — they do not compile. CI builds first, Docker packages.
- Image tags are 7-char git SHAs (dev/staging) or `vX.Y.Z` versions (prod) — never edit them manually in `repo-config/` values files; CI and `promote-prod.yml` own them
- `@CrossOrigin(origins = "*")` on `HealthController` is present but never fires in prod (Nginx same-origin proxy)
