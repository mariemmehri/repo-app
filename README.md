# demo-hr — Mini-portail RH (inspiré Sopra HR4YOU)

Application fullstack conteneurisée servant de **charge de validation** pour la pipeline GitOps CI/CD (PFE — Sopra HR Software). C'est un portail RH (SIRH) avec des flux métier réalistes ; le package Java, l'artifactId Maven, les images Docker, les Services/Deployments K8s et la sonde de santé reflètent tous le métier RH (`com.example.hr`, `hr-backend`/`hr-frontend`, `GET /api/health-check`).

Deux flux métier sont simulés :

1. **Demande de congés** — formulaire (type CP/RTT/sans solde, dates, commentaire), calcul automatique des jours ouvrés, workflow de statut (En attente / Validé / Refusé), historique « mes demandes ».
2. **Consultation de bulletins de paie** — liste par mois/année, détail (brut, net, cotisations, cumuls) avec données factices réalistes, téléchargement PDF simulé.

> ⚠️ Toutes les données sont **fictives** et **en mémoire** (aucune base, aucun volume). Elles sont recréées au démarrage du backend et perdues au redémarrage du pod, pour ne rien ajouter qui puisse casser le déploiement.

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

### Employés

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/employees` | Annuaire (5 employés fictifs) |
| GET | `/api/employees/{id}` | Fiche d'un employé |

### Congés

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/leaves?employeeId=1` | Historique « mes demandes » d'un employé |
| POST | `/api/leaves` | Soumettre une demande (jours ouvrés calculés côté serveur) |
| PUT | `/api/leaves/{id}/decision` | Décision manager (valider / refuser) |

Exemple de soumission :

```bash
curl -X POST http://localhost:8081/api/leaves \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"type":"CP","startDate":"2026-09-07","endDate":"2026-09-11","comment":"Test validation"}'
# -> workingDays calculé (5), status "EN_ATTENTE"
```

Exemple de décision :

```bash
curl -X PUT http://localhost:8081/api/leaves/1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision":"VALIDE","decisionComment":"OK manager"}'
```

### Bulletins de paie

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/payslips?employeeId=1` | Liste des bulletins (3 par employé) |
| GET | `/api/payslips/{id}` | Détail (lignes de cotisations, cumuls) |
| GET | `/api/payslips/{id}/download` | Téléchargement PDF simulé (`application/pdf`) |

```bash
curl -OJ http://localhost:8081/api/payslips/1/download   # -> bulletin_SHR-0001_2026-03.pdf
```

---

## Données de démonstration

- **5 employés** (`SHR-0001` → `SHR-0005`) dans différents départements, avec soldes CP/RTT.
- **Historique de congés** varié : demandes validées, refusées, en attente (répartis sur les 5 employés).
- **3 bulletins par employé** (Mars, Avril, Mai 2026) avec brut/net/cotisations et cumuls annuels.

Toutes ces données sont générées au démarrage par [`HrDataStore`](backend/src/main/java/com/example/hr/service/HrDataStore.java).

---

## Vérifier chaque étape du flux via les logs

Le backend émet des logs préfixés pour suivre la validation pas à pas :

| Préfixe | Émis lors de |
|---------|--------------|
| `[SEED]` | Chargement du jeu de données au démarrage (employés, congés, bulletins) |
| `[API]` | Chaque appel REST reçu (méthode + URL + paramètres) |
| `[LEAVE][CALC]` | Calcul des jours ouvrés (dates → nombre de jours) |
| `[LEAVE][SUBMIT]` | Soumission d'une demande (avant/après enregistrement) |
| `[LEAVE][DECISION]` | Validation / refus d'une demande |
| `[LEAVE][HISTORY]` | Consultation de l'historique |
| `[PAY]` / téléchargement | Chargement / génération du PDF d'un bulletin |

Au démarrage, un encadré `Demo RH … backend PRÊT` récapitule tous les endpoints.

Côté frontend, la console navigateur affiche des logs `[HR-UI]` / `[HR-UI][CONGES]` / `[HR-UI][PAIE]` pour tracer les actions utilisateur.

Suivre les logs du backend en local :

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
| Artefact backend | `backend/target/*.jar` (`mvn verify -q` OK, aucun test → pas d'échec) |
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
