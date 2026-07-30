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

**Package root:** `com.example.hr` (Maven artifactId `hr-backend`, Spring Boot 3.2.0, Java 17)
**Port:** 8081

The backend has a real Postgres connection (Spring Data JPA + `org.postgresql:postgresql` driver pinned `42.7.2`) and now **does have a business domain**: `Employee` and `LeaveRequest` are real `@Entity` classes (tables `employees`/`leave_requests`, `ddl-auto=update`), each with a repository (`EmployeeRepository`, `LeaveRequestRepository`), plus `EmployeeController` (`GET /api/employees`, `GET /api/employees/{id}`) and `LeaveController` (`GET /api/leaves?employeeId=`, `POST /api/leaves`, `PUT /api/leaves/{id}/decision`) backed by `LeaveService` (working-days calc via `WorkingDaysCalculator`, status workflow `EN_ATTENTE`/`VALIDE`/`REFUSE` via the `LeaveStatus` enum). `DataSeeder` (an `ApplicationRunner`) inserts 3 demo employees on first boot only (skips if the table is non-empty) — **not 5**, and **not in-memory**: README.md's "5 employees, in-memory, lost on pod restart" description is stale, superseded by this real-Postgres seeder.

There is still **no payslip/bulletin domain on the backend** — no `Payslip` entity, repository, or controller exist despite README.md documenting `/api/payslips`, `/api/payslips/{id}`, and `/api/payslips/{id}/download` in detail. Those three routes are backend-only vaporware right now.

`HealthController` (separate from the domain controllers, package root not `web/`) serves `GET /api/health-check` (K8s readiness/liveness probe target, returns `200 []`), `GET /api/health` (explicit status), and `GET /api/db-health` (writes + counts a row via `HealthCheckRepository`/`HealthCheck` — a JPA entity that exists purely to prove DB connectivity, unrelated to the Employee/LeaveRequest domain). `spring.datasource.*`/`spring.jpa.*` live in `application.properties`, driven by `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` env vars locally (defaults point at `docker-compose`'s `postgres` service) and overridden in staging by the `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD` env vars the Helm chart injects from the CNPG-generated secret `pg-staging-app` (see `repo-config`'s `charts/hr-app/templates/deployment-backend.yaml`).

Tests (`mvn verify`) require a reachable Postgres: `HrApplicationTests` (context load + a health-check round trip) and `HealthControllerDbHealthTest` (`RANDOM_PORT`, hits `/api/db-health` over HTTP). Neither test covers `EmployeeController`/`LeaveController` yet.

## Frontend

**Framework:** Angular 17 (standalone components, no NgModule), Node 20
**Port:** 80 (Nginx), proxies `/api/*` → `http://hr-backend:8081`

Key files: `src/app/hr.service.ts` (HttpClient wrapper — `getEmployees`, `getLeaves`/`submitLeave`, plus `getPayslips`/`getPayslip`/`payslipDownloadUrl`), `src/app/models.ts` (TS interfaces: `Employee`, `LeaveRequest`, `Payslip`/`PayslipLine`), `src/app/app.component.ts` (single standalone component, ~160 lines — the whole UI lives here, no routing/sub-components).

Note: `hr.service.ts` still calls `/api/payslips*` routes that do not exist on the backend (see above) — those calls will fail (404) until a payslip controller is added; the employee and leave calls, by contrast, are now fully wired end-to-end.

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
mvn spring-boot:run              # needs Postgres reachable (DB_HOST etc.) — see docker-compose
mvn verify                       # compile + test — what CI runs (mvn verify -q)
mvn test -Dtest=ClassName#methodName   # single test
```
`HrApplicationTests` and `HealthControllerDbHealthTest` both require a live Postgres (`docker compose up postgres -d` from `repo-app/` first, or CI's `postgres:16-alpine` service container) — they don't mock the DB.

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
| GET | `/api/health` | `{"status":"UP","app":"demo-hr",...}` |
| GET | `/api/health-check` | K8s readiness/liveness probe target — returns `200 []` |
| GET | `/api/db-health` | `{"status":"UP","database":"postgresql","totalChecks":N}` (200) or `{"status":"DOWN",...}` (503) — real JPA insert+count round trip |
| GET | `/api/employees` | Directory — all seeded employees |
| GET | `/api/employees/{id}` | One employee, 404 if not found |
| GET | `/api/leaves?employeeId=` | Leave-request history for an employee |
| POST | `/api/leaves` | Submit a leave request — server computes `workingDays`, sets status `EN_ATTENTE` |
| PUT | `/api/leaves/{id}/decision` | Manager decision — `VALIDE`/`REFUSE` + optional comment |
| GET/POST | `/api/payslips*` (README-documented) | **Not implemented** — frontend calls these, backend has no controller for them (404) |

## Key Constraints

- `--legacy-peer-deps` is always required for `npm install` (Angular 17 peer-dep conflicts)
- Dockerfiles expect pre-built artifacts (JAR / dist/) — they do not compile. CI builds first, Docker packages.
- Image tags are 7-char git SHAs (dev/staging) or `vX.Y.Z` versions (prod) — never edit them manually in `repo-config/` values files; CI and `promote-prod.yml` own them
- `@CrossOrigin(origins = "*")` is present on every controller (`HealthController`, `EmployeeController`, `LeaveController`) but never fires in any deployed topology (Nginx same-origin proxy in front of the backend)
- **Payslip gap**: README.md documents a full `/api/payslips` feature set (list/detail/PDF download) and the frontend (`hr.service.ts`, `models.ts`) already has the client-side code for it, but there is no `Payslip` entity/repository/controller in `backend/src/main/java/com/example/hr` — don't assume the README's payslip section reflects working code; verify against the backend source before relying on it
- `DataSeeder` seeds **3** demo employees (`SHR-0001`-`SHR-0003`) on first boot only, not the 5 described in README.md — it's a real Postgres insert (`ApplicationRunner`, skipped if the table is already non-empty), not in-memory/ephemeral data as README.md's intro claims
- `scripts/` exists but is currently empty — nothing to invoke from there
