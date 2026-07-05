# Frontend — SPA Angular 17 (Portail RH)

Interface du mini-portail RH (inspiré Sopra HR4YOU), construite avec Angular 17 (standalone components), servie par Nginx en production. Deux vues métier : **Mes congés** et **Mes bulletins**.

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

### Composant principal `AppComponent` (standalone)

Le frontend est un **standalone component** Angular 17 — pas de `NgModule`. Contrairement à l'app d'origine (tout inline), le code est découpé pour rester lisible :

```
src/app/
├── app.component.ts     # Logique : sélection employé, vues congés / bulletins
├── app.component.html   # Template (header, sidebar, formulaire, tableaux, détail bulletin)
├── models.ts            # Interfaces TS alignées sur les DTO backend (Employee, LeaveRequest, Payslip…)
└── hr.service.ts        # Service HttpClient : appels /api/employees, /api/leaves, /api/payslips

src/
├── index.html
├── main.ts              # bootstrapApplication(AppComponent)
└── styles.css           # Styles GLOBAUX (déclarés dans angular.json)
```

> Les styles sont dans `src/styles.css` (global) plutôt qu'en `styles: []` inline du composant, afin de rester sous le budget `anyComponentStyle` (4 kb) imposé par la configuration production d'Angular.

### Fonctionnalités

- **Sélecteur d'employé** (header) — bascule tout le contexte affiché.
- **Vue Mes congés** : formulaire (type CP/RTT/sans solde, dates, commentaire) avec **estimation live** des jours ouvrés, bouton de soumission, tableau « historique de mes demandes » avec badges de statut (En attente / Validé / Refusé).
- **Vue Mes bulletins** : liste par période, vue détail (lignes de cotisations, cumuls annuels), bouton **télécharger en PDF**.

### Flux de données

```
Utilisateur (UI)
     │ (ngModel, click)
     ▼
AppComponent ──> HrService
     │ HttpClient.get/post/put('/api/employees | /api/leaves | /api/payslips')
     ▼
Nginx (proxy /api/ → todo-backend:8081)
     ▼
Spring Boot REST API
```

Le frontend utilise des **URLs relatives** (`/api/...`). Nginx intercepte toutes les requêtes `/api/` et les redirige vers le service backend, de façon identique :
- En Docker Compose : Nginx résout `todo-backend` via le réseau Docker
- En Kubernetes : Nginx résout `todo-backend` via le service ClusterIP K8s

Aucune variable d'environnement ni configuration CORS n'est nécessaire dans le code Angular.

### Logs de validation (console navigateur)

Chaque action utilisateur émet un log préfixé pour suivre le flux pendant la validation :

| Préfixe | Émis lors de |
|---------|--------------|
| `[HR-UI]` | Démarrage, chargement des employés, changement d'employé/de vue |
| `[HR-UI][CONGES]` | Chargement historique, soumission d'une demande |
| `[HR-UI][PAIE]` | Chargement liste, ouverture détail, téléchargement PDF |

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

Le bloc `try_files $uri $uri/ /index.html` est essentiel pour le routing Angular : sans lui, un rechargement de page sur une route interne renverrait une 404 Nginx.

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

| Package | Usage |
|---------|-------|
| `@angular/core` | Framework Angular |
| `@angular/common` | `CommonModule` (`*ngFor`, `*ngIf`) + `HttpClient` |
| `@angular/forms` | `FormsModule` (`[(ngModel)]`) |
| `@angular/platform-browser` | Bootstrap du navigateur |
| `@angular/router` | Routing (inclus, non configuré) |
| `rxjs` | Programmation réactive (Observables) |
| `zone.js` | Détection de changement Angular |

| Commande | Action |
|----------|--------|
| `npm start` | `ng serve --host 0.0.0.0` (dev server) |
| `npm run build` | `ng build --configuration production` |
| `npm run watch` | Build en mode watch (développement) |
