# demo-hr — Mini-portail RH (inspiré Sopra HR4YOU)

Application fullstack conteneurisée servant de **charge de validation** pour la pipeline GitOps CI/CD (PFE — Sopra HR Software). Le package Java, l'artifactId Maven, les images Docker, les Services/Deployments K8s et la sonde de santé reflètent le nom du projet (`com.example.hr`, `hr-backend`/`hr-frontend`, `GET /api/health-check`).

> ⚠️ Le backend n'expose aujourd'hui qu'un endpoint de santé — aucune donnée métier, aucune persistance (l'ancien domaine RH avec JPA + PostgreSQL a été retiré).

---

## Contenu

```
repo-app/
├── backend/    # API REST Spring Boot (Java 17) — package com.example.hr
├── frontend/   # SPA Angular 17 (Nginx) — portail RH
├── docker-compose.yml         # Dev local
└── .github/workflows/ci.yml   # Pipeline CI/CD
```

Le package Java est `com.example.hr` (artifactId Maven `hr-backend`), les images Docker et les Services/Deployments K8s sont `hr-backend` / `hr-frontend` (voir `ci.yml` et le chart Helm `charts/hr-app`).

---

## Démarrage rapide — Développement local

### Avec Docker Compose (recommandé)

```bash
cd repo-app
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend (portail RH) | http://localhost:80 |
| Backend API (santé)   | http://localhost:8081/api/health |

Nginx proxifie `/api/*` vers `hr-backend:8081` en local (nom du service docker-compose) — le frontend appelle des chemins relatifs, aucun CORS à gérer.

### Sans Docker

```bash
# Backend
cd backend && mvn spring-boot:run        # port 8081

# Frontend (dans un autre terminal)
cd frontend && npm install --legacy-peer-deps && npm start   # port 4200 (ng serve)
```

---

## Endpoints à tester

### Santé (utilisés par les sondes Kubernetes — NE PAS supprimer)

| Méthode | URL | Rôle |
|---------|-----|------|
| GET | `/api/health-check` | Route interrogée par les probes readiness/liveness du chart Helm. Renvoie `[]` (200). |
| GET | `/api/health` | État applicatif : `{"status":"UP","app":"demo-hr"}` |

```bash
curl http://localhost:8081/api/health
```

---

## Vérifier le démarrage via les logs

```bash
docker compose logs -f backend
```

Sur le cluster :

```bash
kubectl logs -n staging deploy/hr-backend -f
```

---

## Renommage RH (coordonné entre `repo-app` et `repo-config`)

| Élément | Valeur |
|---------|--------|
| Artefact backend | `backend/target/*.jar` (`mvn verify -q` OK) |
| Artefact frontend | `frontend/dist/hr-frontend/browser` |
| Port backend | `8081` |
| Port frontend | `80` |
| Package Java / artifactId Maven | `com.example.hr` / `hr-backend` |
| Nom images | `hr-backend`, `hr-frontend` |
| Nom services/deployments K8s | `hr-backend`, `hr-frontend` |
| Sonde readiness/liveness | `GET /api/health-check` (renvoie 200) |
| Proxy Nginx (docker-compose + K8s) | `/api/` → `http://hr-backend:8081` |

Le renommage a été appliqué de façon coordonnée dans `ci.yml` + `pom.xml` + le code Java (`repo-app`), et le chart Helm `charts/hr-app` (`repo-config`) : `docker-compose.yml`, `nginx.conf` et les Dockerfiles ont été mis à jour en conséquence.

---

## Pour aller plus loin

- [backend/README.md](backend/README.md) — API Spring Boot
- [frontend/README.md](frontend/README.md) — Angular / Nginx
- La documentation détaillée de la pipeline CI/CD (jobs, WIF, Trivy, GitOps) reste valable — voir `.github/workflows/ci.yml`.
