# Backend — API REST Spring Boot (demo-hr)

API REST minimale (Spring Boot 3.2, Java 17). Le backend n'expose aucune donnée métier, mais dispose d'une vraie connexion Postgres via Spring Data JPA, utilisée par l'endpoint `/api/db-health` pour prouver que la base est joignable.

---

## Stack

| Composant | Version |
|-----------|---------|
| Java | 17 (Temurin) |
| Spring Boot | 3.2.0 |
| Build | Maven (`com.example:hr-backend`) |
| Port | 8081 |
| Stockage | PostgreSQL 16 via Spring Data JPA (Hikari, `ddl-auto=update`) |
| Image de base | `eclipse-temurin:17-jre-alpine` |

> Le package Java est `com.example.hr`, l'artifactId Maven `hr-backend` et l'image Docker `hr-backend`.

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
docker build -t hr-backend:local .

# Lancer le container
docker run -p 8081:8081 hr-backend:local
```

### Via Docker Compose (recommandé)

```bash
# depuis la racine de repo-app/
docker compose up --build
```

---

## Endpoints API

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/api/health-check` | Route interrogée par les probes readiness/liveness du chart Helm. Renvoie `[]` (200). NE PAS supprimer. |
| `GET` | `/api/health` | État applicatif : `{"status":"UP","app":"demo-hr"}` |
| `GET` | `/api/db-health` | Écrit puis compte une ligne via JPA (`HealthCheckRepository`) : `{"status":"UP","database":"postgresql","totalChecks":N}` (200), ou `{"status":"DOWN",...}` (503) si Postgres est injoignable. |

### Exemple cURL

```bash
curl http://localhost:8081/api/health
curl http://localhost:8081/api/db-health
```

---

## Structure du code

```
src/main/java/com/example/hr/
├── HrApplication.java              # Point d'entrée @SpringBootApplication + log de démarrage
├── HealthController.java           # Endpoints santé : /api/health-check (sonde K8s), /api/health, /api/db-health
├── model/
│   └── HealthCheck.java            # Entité JPA minimale utilisée uniquement pour prouver la connectivité Postgres
└── repository/
    └── HealthCheckRepository.java  # JpaRepository<HealthCheck, Long>

src/main/resources/
└── application.properties      # server.port=8081 + spring.datasource.* / spring.jpa.*
```

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

Ces tests **nécessitent une vraie instance Postgres joignable** (variables `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`, défauts `localhost:5432/hrdb/hruser/hrpass`) :
- `HrApplicationTests` : le contexte Spring démarre (donc la connexion Postgres s'établit) + un aller-retour JPA réel (`save` + `count` sur `HealthCheckRepository`).
- `HealthControllerDbHealthTest` : appelle `/api/db-health` sur un serveur réel (`webEnvironment=RANDOM_PORT`) et vérifie `status=UP`.

Localement, démarrer Postgres d'abord (`docker compose up postgres -d` depuis la racine `repo-app/`, ou toute instance Postgres 16 locale) avant `mvn test`/`mvn verify`. En CI, le job `backend-ci` démarre un service container `postgres:16-alpine` avant `mvn verify -q`.

---

## Dépendances (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.2</version>
    <scope>runtime</scope>
</dependency>
```

Le driver PostgreSQL est épinglé à `42.7.2` (corrige CVE-2024-1597). `tomcat.version` est épinglé à `10.1.55` pour corriger des CVE détectées par Trivy lors du scan d'image.

---

## Limitations connues (assumées pour la démo)

- **Pas de logique métier** : la persistance Postgres est branchée (JPA + `/api/db-health`) mais l'ancien domaine RH (employés, congés, bulletins de paie) n'a pas été réintroduit — aucune entité/repository/controller métier n'existe encore.
- **`spring.jpa.hibernate.ddl-auto=update`** : acceptable pour la démo (une seule table `health_check`, auto-créée) ; à remplacer par des migrations versionnées (Flyway/Liquibase) avant d'y stocker de vraies données métier.
- **CORS ouvert** : `@CrossOrigin(origins = "*")` acceptable en démo ; en production Nginx proxifie `/api/*` donc les appels sont same-origin.
