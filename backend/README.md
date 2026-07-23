# Backend — API REST Spring Boot (demo-hr)

API REST minimale (Spring Boot 3.2, Java 17). Le backend n'expose aujourd'hui qu'un endpoint de santé — aucune donnée métier, aucune persistance.

---

## Stack

| Composant | Version |
|-----------|---------|
| Java | 17 (Temurin) |
| Spring Boot | 3.2.0 |
| Build | Maven (`com.example:hr-backend`) |
| Port | 8081 |
| Stockage | Aucun (pas de base, pas de volume) |
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

### Exemple cURL

```bash
curl http://localhost:8081/api/health
```

---

## Structure du code

```
src/main/java/com/example/hr/
├── HrApplication.java          # Point d'entrée @SpringBootApplication + log de démarrage
└── HealthController.java       # Endpoints santé : /api/health-check (sonde K8s) + /api/health

src/main/resources/
└── application.properties      # server.port=8081
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

`HrApplicationTests` vérifie uniquement que le contexte Spring démarre — aucune dépendance externe requise.

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

- **Pas de persistance et pas de logique métier** : le backend n'expose qu'un health check ; l'ancien domaine RH (employés, congés, bulletins de paie, JPA + PostgreSQL) a été retiré.
- **CORS ouvert** : `@CrossOrigin(origins = "*")` acceptable en démo ; en production Nginx proxifie `/api/*` donc les appels sont same-origin.
