# Démo locale — repo-app (WSL Ubuntu + Docker)

Workflow simple pour faire tourner l'application en local et la présenter devant le jury.

---

## 1. Ouvrir WSL et se placer dans le projet

```powershell
wsl -d Ubuntu
```

```bash
cd "/mnt/c/Users/wassi/Downloads/stage pfe/repo-app"
```

## 2. Lancer toute l'application

```bash
docker compose up -d --build
docker compose ps
```

Attendre que les 2 services soient `Up` (backend `Up`, frontend `healthy`) — une trentaine de secondes.

## 3. Vérifier que tout répond

```bash
curl http://localhost:8081/api/health
curl http://localhost/api/health
```

Les deux doivent renvoyer `{"status":"UP","app":"demo-hr",...}`.

## 4. Ouvrir l'application

Dans un navigateur Windows : **http://localhost**

---

## 5. Arrêt après la démo

```bash
docker compose down
```

---

## En cas de souci pendant la démo

| Symptôme | Solution rapide |
|---|---|
| `port is already allocated` | `docker compose down` puis relancer `docker compose up -d --build` |
| Page blanche / API en erreur | `docker compose ps` → vérifier que les 2 services sont bien `Up`/`healthy` |
| Rien ne s'affiche sur `localhost` | Vérifier que Docker Desktop / le moteur Docker de la distro WSL tourne (`docker ps`) |
