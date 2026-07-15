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

Attendre que les 3 services soient `Up` (postgres `healthy`, backend `Up`, frontend `healthy`) — une trentaine de secondes.

## 3. Vérifier que tout répond

```bash
curl http://localhost:8081/api/health
curl http://localhost/api/health
```

Les deux doivent renvoyer `{"status":"UP","app":"demo-hr",...}`.

## 4. Ouvrir l'application

Dans un navigateur Windows : **http://localhost**

---

## Déroulé de la démo devant le jury

**a) Annuaire employés** — onglet employés : 5 employés fictifs (SHR-0001 → SHR-0005), départements variés.

**b) Demande de congés**
- Onglet "Congés" → soumettre une demande (dates + type CP/RTT/sans solde)
- Montrer le calcul automatique des jours ouvrés
- Montrer la demande apparaître "En attente" dans l'historique
- Enchaîner sur la décision manager (Valider/Refuser) et montrer le changement de statut

**c) Bulletins de paie**
- Onglet "Bulletins" → ouvrir un bulletin (brut/net/cotisations/cumuls)
- Télécharger le PDF

**d) Persistance (argument clé du choix Postgres)**
```bash
docker compose restart backend
```
Recharger la page → les mêmes données sont toujours là (pas de perte au redémarrage, contrairement à un stockage in-memory).

**e) (optionnel) Suivre les logs en direct pendant la démo**
```bash
docker compose logs -f backend
```
Préfixes visibles : `[SEED]`, `[API]`, `[LEAVE][CALC]`, `[LEAVE][SUBMIT]`, `[LEAVE][DECISION]`, `[PAY]`.

---

## 5. Arrêt après la démo

```bash
docker compose down
```
(garde les données ; `docker compose down -v` pour repartir d'une base vierge)

---

## En cas de souci pendant la démo

| Symptôme | Solution rapide |
|---|---|
| `port is already allocated` | `docker compose down` puis relancer `docker compose up -d --build` |
| Page blanche / API en erreur | `docker compose ps` → vérifier que les 3 services sont bien `Up`/`healthy` |
| Rien ne s'affiche sur `localhost` | Vérifier que Docker Desktop / le moteur Docker de la distro WSL tourne (`docker ps`) |
