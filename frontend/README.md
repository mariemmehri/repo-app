# Frontend — SPA Angular 17

Interface utilisateur Todo construite avec Angular 17 (standalone components), servie par Nginx en production.

---

## Stack

| Composant | Version |
|-----------|---------|
| Angular | 17.0.0 |
| TypeScript | 5.2.2 |
| RxJS | 7.8.0 |
| Serveur | Nginx Alpine |
| Port | 80 |
| Build | Angular CLI (`ng build --configuration production`) |

---

## Développement local

### Serveur de développement Angular

```bash
cd frontend
npm install --legacy-peer-deps
npm start
# Disponible sur http://localhost:4200
```

> `--legacy-peer-deps` est nécessaire pour contourner les conflits de peer dependencies d'Angular 17.

### Build de production

```bash
npm run build
# Génère frontend/dist/todo-frontend/browser/
```

### Via Docker Compose (application complète)

```bash
# depuis la racine de todo-app/
docker compose up --build
# Frontend sur http://localhost:80
```

---

## Architecture du frontend

### Composant unique `AppComponent`

Le frontend est un **standalone component** Angular 17 — pas de `NgModule`. Tout est dans `app.component.ts` :
- Template HTML inline
- Styles CSS inline
- Logique HTTP via `HttpClient`

```
AppComponent
  ├── Imports : CommonModule, FormsModule, HttpClientModule
  ├── State  : todos: Todo[], newTitle: string
  └── Methods: loadTodos(), addTodo(), toggleTodo(), deleteTodo()
```

### Flux de données

```
Utilisateur (UI)
     │ (ngModel, click, keyup.enter)
     ▼
AppComponent
     │ HttpClient.get/post/put/delete('/api/todos')
     ▼
Nginx (proxy /api/ → todo-backend:8081)
     │
     ▼
Spring Boot REST API
```

Le frontend utilise des **URLs relatives** (`/api/todos`). Nginx intercepte toutes les requêtes commençant par `/api/` et les redirige vers le service backend. Cela fonctionne de façon identique :
- En Docker Compose : Nginx résout `todo-backend` via le réseau Docker
- En Kubernetes : Nginx résout `todo-backend` via le service ClusterIP K8s

Aucune variable d'environnement ni configuration CORS n'est nécessaire dans le code Angular.

---

## Configuration Nginx (`nginx.conf`)

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;

    # Proxy API → backend Spring Boot
    location /api/ {
        proxy_pass http://todo-backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # SPA fallback — toutes les routes → index.html
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Le bloc `try_files $uri $uri/ /index.html` est essentiel pour le routing Angular : sans lui, un rechargement de page sur `/todos/1` renverrait une 404 Nginx.

---

## Dockerfile — Pattern Build-Once

```dockerfile
FROM nginx:alpine

# Utilisateur non-root
RUN addgroup -S app && adduser -S app -G app

# dist/ compilé et uploadé par GitHub Actions Job 2
COPY dist/todo-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R app:app /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Le Dockerfile reçoit le répertoire `dist/` déjà compilé par le CI. Il ne réinstalle pas les dépendances npm ni ne relance `ng build`.

---

## Dépendances (`package.json`)

### Dépendances applicatives

| Package | Usage |
|---------|-------|
| `@angular/core` | Framework Angular |
| `@angular/common` | `CommonModule` (`*ngFor`, `*ngIf`) |
| `@angular/forms` | `FormsModule` (`[(ngModel)]`) |
| `@angular/platform-browser` | Bootstrap du navigateur |
| `@angular/router` | Routing (inclus, non configuré) |
| `rxjs` | Programmation réactive (Observables) |
| `zone.js` | Détection de changement Angular |

### Scripts

| Commande | Action |
|----------|--------|
| `npm start` | `ng serve --host 0.0.0.0` (dev server) |
| `npm run build` | `ng build --configuration production` |
| `npm run watch` | Build en mode watch (développement) |

---

## Interface utilisateur

L'application affiche :
- Un champ de saisie pour ajouter une tâche (soumission via Entrée ou bouton)
- La liste des tâches avec checkbox de complétion et bouton de suppression
- Un style visuel qui barre les tâches complétées
- Un message vide si aucune tâche n'existe

Toutes les interactions sont synchronisées en temps réel avec le backend via `HttpClient`.
