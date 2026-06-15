# Backend — API REST Spring Boot

API REST CRUD pour la gestion de tâches Todo, développée avec Spring Boot 3.2 et Java 17.

---

## Stack

| Composant | Version |
|-----------|---------|
| Java | 17 (Temurin) |
| Spring Boot | 3.2.0 |
| Build | Maven |
| Port | 8081 |
| Stockage | In-memory (`ArrayList`) |
| Image de base | `eclipse-temurin:17-jre-alpine` |

---

## Développement local

### Sans Docker (IDE ou terminal)

```bash
cd backend

# Compiler et lancer les tests
mvn verify

# Démarrer le serveur
mvn spring-boot:run

# API disponible sur http://localhost:8081/api/todos
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

Base URL : `http://localhost:8081/api/todos`

| Méthode | Chemin | Description | Corps |
|---------|--------|-------------|-------|
| `GET` | `/api/todos` | Lister toutes les tâches | — |
| `POST` | `/api/todos` | Créer une tâche | `{"title": "...", "completed": false}` |
| `PUT` | `/api/todos/{id}` | Modifier une tâche | `{"completed": true}` |
| `DELETE` | `/api/todos/{id}` | Supprimer une tâche | — |

### Exemples cURL

```bash
# Lister
curl http://localhost:8081/api/todos

# Créer
curl -X POST http://localhost:8081/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Apprendre Kubernetes", "completed": false}'

# Compléter la tâche id=1
curl -X PUT http://localhost:8081/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'

# Supprimer
curl -X DELETE http://localhost:8081/api/todos/1
```

---

## Structure du code

```
src/main/java/com/example/todo/
├── TodoApplication.java    # Point d'entrée @SpringBootApplication
├── Todo.java               # Modèle de données POJO (id, title, completed)
└── TodoController.java     # REST controller CRUD complet

src/main/resources/
└── application.properties  # server.port=8081
```

### `Todo.java`

POJO simple avec trois champs : `Long id`, `String title`, `boolean completed`. Getters/setters manuels (pas de Lombok pour minimiser les dépendances).

### `TodoController.java`

- Stockage en mémoire : `List<Todo> todos = new ArrayList<>()`
- Compteur auto-incrémenté thread-safe : `AtomicLong counter`
- Préchargé avec 3 tâches au démarrage pour faciliter la démonstration
- `@CrossOrigin(origins = "*")` — CORS ouvert pour le développement local

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

Le Dockerfile ne compile pas le code. Le CI (GitHub Actions Job 1) compile et teste d'abord, puis télécharge le JAR avant le `docker build`. Cela garantit que l'image contient exactement le binaire validé par les tests.

---

## Tests

```bash
mvn test           # uniquement les tests unitaires
mvn verify         # compile + tests + vérifications complètes
```

Les rapports XML sont dans `target/surefire-reports/` et uploadés en tant qu'artefact GitHub Actions (rétention 7 jours).

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

## Limitations connues

- **Pas de persistance** : les données sont perdues au redémarrage du pod. Une vraie implémentation utiliserait Spring Data JPA + PostgreSQL.
- **Pas de Spring Boot Actuator** : les endpoints `/actuator/health` n'existent pas — les liveness/readiness probes Kubernetes ne peuvent pas être configurées correctement sans ce module.
- **CORS ouvert** : `@CrossOrigin(origins = "*")` est acceptable en développement, pas en production (restreindre à l'origine du frontend).
