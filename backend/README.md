# Backend — API REST Spring Boot (demo-hr)

API REST du mini-portail RH (inspiré Sopra HR4YOU), développée avec Spring Boot 3.2 et Java 17. Elle expose deux flux métier — **demandes de congés** et **bulletins de paie** — sur des données factices en mémoire.

---

## Stack

| Composant | Version |
|-----------|---------|
| Java | 17 (Temurin) |
| Spring Boot | 3.2.0 |
| Build | Maven (`com.example:todo-backend`) |
| Port | 8081 |
| Stockage | In-memory (aucune base, aucun volume) |
| Image de base | `eclipse-temurin:17-jre-alpine` |

> Le package Java reste `com.example.todo` et l'artifactId Maven `todo-backend` : le nom du JAR (donc le `COPY target/*.jar` du Dockerfile) et le nom d'image restent inchangés pour ne pas casser la pipeline. Le métier RH vit dans le sous-package `com.example.todo.hr`.

---

## Développement local

### Sans Docker (IDE ou terminal)

```bash
cd backend

# Compiler et lancer les tests (exactement ce que fait le CI)
mvn verify

# Démarrer le serveur
mvn spring-boot:run

# Santé disponible sur http://localhost:8081/api/health
```

### Avec Docker uniquement

```bash
# Compiler d'abord (le Dockerfile ne compile pas)
mvn package -DskipTests

# Construire l'image
docker build -t todo-backend:local .

# Lancer le container
docker run -p 8081:8081 todo-backend:local
```

### Via Docker Compose (recommandé)

```bash
# depuis la racine de todo-app/
docker compose up --build
```

---

## Endpoints API

### Santé (interrogés par les sondes Kubernetes — NE PAS supprimer)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/api/todos` | **Route historique** utilisée par les probes readiness/liveness du chart Helm. Renvoie `[]` (200). |
| `GET` | `/api/health` | État applicatif : `{"status":"UP","app":"demo-hr"}` |

### Employés

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/api/employees` | Annuaire (5 employés fictifs) |
| `GET` | `/api/employees/{id}` | Fiche d'un employé |

### Congés

| Méthode | Chemin | Description | Corps |
|---------|--------|-------------|-------|
| `GET` | `/api/leaves?employeeId=1` | Historique « mes demandes » | — |
| `POST` | `/api/leaves` | Soumettre une demande (jours ouvrés calculés serveur) | `{"employeeId":1,"type":"CP","startDate":"2026-09-07","endDate":"2026-09-11","comment":"..."}` |
| `PUT` | `/api/leaves/{id}/decision` | Décision manager | `{"decision":"VALIDE","decisionComment":"..."}` |

`type` ∈ `CP`, `RTT`, `SANS_SOLDE` — `decision` ∈ `VALIDE`, `REFUSE`.

### Bulletins de paie

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/api/payslips?employeeId=1` | Liste des bulletins (3 par employé) |
| `GET` | `/api/payslips/{id}` | Détail (lignes de cotisations, cumuls) |
| `GET` | `/api/payslips/{id}/download` | Téléchargement PDF simulé (`application/pdf`) |

### Exemples cURL

```bash
# Santé
curl http://localhost:8081/api/health

# Soumettre une demande de congé (jours ouvrés calculés automatiquement)
curl -X POST http://localhost:8081/api/leaves \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"type":"CP","startDate":"2026-09-07","endDate":"2026-09-11","comment":"Test"}'

# Valider la demande id=1
curl -X PUT http://localhost:8081/api/leaves/1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision":"VALIDE","decisionComment":"OK manager"}'

# Télécharger un bulletin en PDF
curl -OJ http://localhost:8081/api/payslips/1/download
```

---

## Structure du code

```
src/main/java/com/example/todo/
├── TodoApplication.java        # Point d'entrée @SpringBootApplication + log de démarrage
├── TodoController.java         # Endpoints santé : /api/todos (sonde K8s) + /api/health
└── hr/                         # ── Métier RH ──
    ├── model/
    │   ├── Employee.java
    │   ├── LeaveRequest.java   # + LeaveType (CP/RTT/SANS_SOLDE), LeaveStatus (EN_ATTENTE/VALIDE/REFUSE)
    │   ├── Payslip.java        # + PayslipLine (lignes de cotisations)
    │   └── ...
    ├── service/
    │   ├── HrDataStore.java            # Dépôt in-memory + seed des données de démo
    │   ├── WorkingDaysCalculator.java  # Calcul jours ouvrés (hors WE + fériés FR fixes)
    │   ├── LeaveService.java           # Logique congés (soumission + décision)
    │   └── PayslipFactory.java         # Génération de bulletins réalistes
    └── web/
        ├── EmployeeController.java
        ├── LeaveController.java
        └── PayslipController.java      # inclut la génération du PDF simulé

src/main/resources/
└── application.properties      # server.port=8081
```

### Données de démonstration

Générées au démarrage par `HrDataStore` (voir logs `[SEED]`) :
- **5 employés** (`SHR-0001` → `SHR-0005`) dans différents départements, avec soldes CP/RTT
- **10 demandes de congé** variées (validées / refusées / en attente)
- **15 bulletins** (3 par employé : Mars, Avril, Mai 2026)

Les données sont **perdues au redémarrage** du pod — c'est volontaire : aucune dépendance externe (DB, volume) n'est introduite, pour ne rien ajouter qui puisse fragiliser le déploiement.

### Génération du PDF

Le téléchargement (`/api/payslips/{id}/download`) construit à la main un PDF 1.4 minimal (texte) — pas de dépendance iText/PDFBox, ce qui garde la surface d'attaque scannée par Trivy identique à l'origine (une seule dépendance applicative).

---

## Logs de validation

Le backend émet des logs préfixés pour suivre chaque étape :

| Préfixe | Émis lors de |
|---------|--------------|
| `[SEED]` / `[SEED][EMP/LEAVE/PAY]` | Chargement du jeu de données au démarrage |
| `[API]` | Chaque appel REST reçu |
| `[LEAVE][CALC]` | Calcul des jours ouvrés |
| `[LEAVE][SUBMIT]` / `[LEAVE][DECISION]` / `[LEAVE][HISTORY]` | Cycle de vie d'une demande |

Un encadré `Demo RH … backend PRÊT` récapitule tous les endpoints au démarrage.

---

## Dockerfile — Pattern Build-Once

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Utilisateur non-root (sécurité)
RUN addgroup -S app && adduser -S app -G app

# Patch des CVE OS-level
RUN apk update && apk upgrade --no-cache

# JAR fourni par le CI — pas de compilation dans le container
COPY target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Le Dockerfile ne compile pas le code. Le CI (GitHub Actions Job 1) compile et teste d'abord, puis télécharge le JAR avant le `docker build` — l'image contient exactement le binaire validé.

---

## Tests

```bash
mvn test     # tests unitaires
mvn verify   # compile + tests + package (ce que lance le CI : mvn verify -q)
```

Il n'y a pas de test unitaire pour le moment : `mvn verify` se limite donc à compiler et packager le JAR (aucun échec possible côté tests). Les rapports éventuels seraient dans `target/surefire-reports/`.

---

## Dépendances (pom.xml)

Une seule dépendance applicative :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

`tomcat.version` est épinglé à `10.1.55` pour corriger des CVE détectées par Trivy lors du scan d'image.

---

## Limitations connues (assumées pour la démo)

- **Pas de persistance** : données en mémoire, perdues au redémarrage du pod. Une vraie implémentation utiliserait Spring Data JPA + PostgreSQL.
- **PDF simulé** : document texte minimal, pas un vrai bulletin mis en page.
- **Jours fériés** : seuls les fériés français à date fixe sont exclus (pas les fériés mobiles type Pâques/Ascension).
- **CORS ouvert** : `@CrossOrigin(origins = "*")` acceptable en démo ; en production Nginx proxifie `/api/*` donc les appels sont same-origin.
