# CLAUDE.md — repo-app

This file provides guidance to Claude Code when working inside `repo-app/`.
The parent `../CLAUDE.md` covers the full platform (infra, config, CI/CD overview).

## Project Structure

```
repo-app/
├── backend/          # Spring Boot 3.2.0 REST API — Java 17, Maven
├── frontend/         # Angular 17 SPA — served by Nginx
└── docker-compose.yml
```

## Backend

**Package root:** `com.example.hr`  
**Port:** 8081

| Layer | Path | Role |
|---|---|---|
| model | `model/` | JPA entities: `Employee`, `LeaveRequest`, `Payslip`, `PayslipLine` |
| repository | `repository/` | Spring Data JPA: `EmployeeRepository`, `LeaveRequestRepository`, `PayslipRepository` |
| service | `service/` | `LeaveService` (business logic), `DataSeeder` (DB init), `PayslipFactory` (PDF), `WorkingDaysCalculator` |
| web | `web/` | REST controllers: `EmployeeController`, `LeaveController`, `PayslipController` |

**Database:** PostgreSQL 16  
- Connection config via env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`  
- Defaults (for local `mvn spring-boot:run`): `localhost:5432/hrdb`, user `hruser`, password `hrpass`  
- Hibernate `ddl-auto=update` — tables created automatically on first boot  
- `DataSeeder` seeds 5 employees + 10 leave requests + 15 payslips only when the DB is empty

**Entities:**
- `Employee` — id, matricule (SHR-XXXX), name, email, department, jobTitle, monthlyGrossSalary, leave balances
- `LeaveRequest` — employeeId (FK), type (CP/RTT/SANS_SOLDE), dates, status (EN_ATTENTE/VALIDE/REFUSE)
- `Payslip` — employeeId (FK), year/month, salary fields; `lines` is `@ElementCollection(EAGER)` in `payslip_lines` table
- `PayslipLine` — `@Embeddable`: label, base, rate, amount

## Frontend

**Framework:** Angular 17 (standalone components, no NgModule)  
**Port:** 80 (Nginx), proxies `/api/*` → `http://hr-backend:8081`

Key files: `src/app/hr.service.ts` (HttpClient), `src/app/models.ts` (TS interfaces)

## Common Commands

### Local dev with Docker Compose
```bash
cd repo-app
docker compose up --build
# Frontend: http://localhost   Backend: http://localhost:8081/api/health
```

### Backend only (requires local Postgres on localhost:5432)
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

### Verify persistence after restart
```bash
docker compose restart backend
curl http://localhost:8081/api/employees   # must return same 5 employees
docker compose logs backend | grep SEED   # expect "seed ignoré" on 2nd boot
```

## API Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/health` | `{"status":"UP"}` |
| GET | `/api/employees` | List all employees |
| GET | `/api/employees/{id}` | Single employee |
| GET | `/api/leaves?employeeId=X` | Leave history |
| POST | `/api/leaves` | Submit leave request |
| PUT | `/api/leaves/{id}/decision` | Approve/reject leave |
| GET | `/api/payslips?employeeId=X` | List payslips |
| GET | `/api/payslips/{id}` | Payslip detail |
| GET | `/api/payslips/{id}/download` | Download payslip PDF |

## Key Constraints

- `--legacy-peer-deps` is always required for `npm install` (Angular 17 peer-dep conflicts)
- `PayslipFactory` is a pure static utility — no Spring context, no changes needed when adding features
- Dockerfiles expect pre-built artifacts (JAR / dist/) — they do not compile. CI builds first, Docker packages.
- Image tags are 7-char git SHAs — never edit them manually in `repo-config/` values files
- `@CrossOrigin(origins = "*")` on controllers is present but never fires in prod (Nginx same-origin proxy)
