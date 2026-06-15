# todo-app — Application fullstack Todo

Application fullstack conteneurisée composée d'un backend Spring Boot et d'un frontend Angular, avec pipeline CI/CD complet vers Google Artifact Registry.

---

## Contenu

```
todo-app/
├── backend/                    # API REST Spring Boot (Java 17)
├── frontend/                   # SPA Angular 17 (Nginx)
├── docker-compose.yml          # Environnement de développement local
└── .github/workflows/ci.yml   # Pipeline CI/CD GitHub Actions
```

---

## Démarrage rapide — Développement local

### Prérequis

- Docker Desktop
- Docker Compose v2

### Lancer l'application complète

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend (Angular via Nginx) | http://localhost:80 |
| Backend API (Spring Boot) | http://localhost:8081/api/todos |

Nginx intercepte tous les appels `/api/*` et les redirige vers le backend. Le frontend utilise des chemins relatifs (`/api/todos`) — aucune configuration CORS à gérer en local ni en production.

### Arrêter

```bash
docker compose down
```

---

## Pipeline CI/CD

Le fichier `.github/workflows/ci.yml` orchestre trois jobs :

### Job 1 — `backend-ci`

Déclenché sur tout push ou PR vers `main`.

```bash
cd backend
mvn verify -q   # compile + tests unitaires
```

Produit :
- Rapport de tests (`target/surefire-reports/`) — conservé 7 jours
- Artefact JAR (`target/*.jar`) — conservé 1 jour (utilisé par Job 3)

### Job 2 — `frontend-ci`

Déclenché en parallèle de Job 1.

```bash
cd frontend
npm install --legacy-peer-deps
npm run build   # ng build --configuration production
```

Produit :
- Artefact `dist/` — conservé 1 jour (utilisé par Job 3)

### Job 3 — `docker-build-push`

Exécuté **uniquement sur push** (pas sur PR) après succès de Job 1 et Job 2.

**Flux d'exécution :**

```
1. Téléchargement artefacts (JAR + dist)
2. Auth GCP via OIDC (Workload Identity Federation — aucune clé JSON)
3. Build image backend   → europe-west1-docker.pkg.dev/.../todo-backend:<SHA>
4. Scan Trivy backend    → échoue si CVE CRITICAL non patchée
5. Push backend          → GAR (tag SHA + tag latest)
6. Build image frontend  → europe-west1-docker.pkg.dev/.../todo-frontend:<SHA>
7. Scan Trivy frontend   → même règle
8. Push frontend
9. Vérification images   → gcloud artifacts docker tags list (validation)
10. Clone repo-config
11. yq patch values-staging.yaml (backend.image.tag + frontend.image.tag = <SHA>)
12. git commit + push → déclenche ArgoCD
```

Le tag d'image est le **short SHA Git** (`${GITHUB_SHA::7}`), assurant la traçabilité complète entre un commit applicatif et l'image déployée sur le cluster.

### Pattern Build-Once, Promote Always

Le CI compile le code UNE fois (Job 1 et 2), stocke les artefacts, et les Dockerfiles se contentent de les empaqueter. Cela garantit que l'image déployée en staging est strictement identique au binaire testé.

```
Code → compile/test (Job 1/2) → JAR/dist → empaqueté dans image Docker → poussé tel quel
                                                                          (pas de recompilation)
```

---

## Variables et secrets GitHub requis

| Type | Nom | Description |
|------|-----|-------------|
| Variable | `GCP_PROJECT_ID` | ID du projet GCP |
| Variable | `GCP_REGION` | Région (`europe-west1`) |
| Variable | `GAR_REPOSITORY` | `<project>/<repo-name>` |
| Variable | `GCP_WORKLOAD_PROVIDER` | Nom complet du WIF provider |
| Variable | `GCP_SERVICE_ACCOUNT` | Email du SA `sa-github-actions` |
| Variable | `CONFIG_REPO` | `owner/repo-config` |
| Secret | `GH_PAT` | PAT GitHub (`contents:write` sur repo-config) |

---

## Sécurité

- **WIF** : authentification sans clé JSON — le token OIDC GitHub est échangé contre un token GCP à durée de vie courte.
- **Trivy** : `exit-code: 1` sur CVE CRITICAL — le pipeline bloque avant le push si une vulnérabilité critique est détectée.
- **Utilisateurs non-root** : les deux Dockerfiles créent un groupe/utilisateur `app` et basculent dessus avant `ENTRYPOINT`.
- **Images Alpine** : `eclipse-temurin:17-jre-alpine` et `nginx:alpine` minimisent la surface d'attaque.
- **Vérification avant GitOps** : le CI vérifie que l'image existe bien dans le registry avant de patcher le repo-config (évite de déployer une image fantôme).

---

## Pour aller plus loin

- [backend/README.md](backend/README.md) — API Spring Boot, endpoints, développement
- [frontend/README.md](frontend/README.md) — Angular, Nginx, build
