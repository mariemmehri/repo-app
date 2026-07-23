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

The backend has no persistence layer — it exposes only a health check. `HealthController` serves `GET /api/health-check` (K8s readiness/liveness probe target) and `GET /api/health` (explicit status). There is no database, no ORM, and no business-domain entities/controllers.

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

## Key Constraints

- `--legacy-peer-deps` is always required for `npm install` (Angular 17 peer-dep conflicts)
- Dockerfiles expect pre-built artifacts (JAR / dist/) — they do not compile. CI builds first, Docker packages.
- Image tags are 7-char git SHAs (dev/staging) or `vX.Y.Z` versions (prod) — never edit them manually in `repo-config/` values files; CI and `promote-prod.yml` own them
- `@CrossOrigin(origins = "*")` on `HealthController` is present but never fires in prod (Nginx same-origin proxy)
