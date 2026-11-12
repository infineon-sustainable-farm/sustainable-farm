# Runbook PT — préparer et rejouer la démo en 15 minutes

Périmètre : module **Product Transformation** (`producttransformation`), démo du
20 novembre. Les autres modules ne sont pas couverts.

Tous les chemins sont relatifs à la racine du dépôt.

---

## 0. Ce que font les scripts

| Script | Rôle | Idempotent |
|---|---|---|
| `scripts/demo/demo-up.sh` | vérifie Docker, démarre Postgres/le backend si besoin, applique le seed PT, vérifie les 11 comptages | oui |
| `scripts/demo/demo-reset.sh` | supprime **uniquement** les lignes `*DEMO*` (aucun `DROP`, aucun `TRUNCATE`) | oui |
| `scripts/demo/demo-down.sh` | `docker compose stop` sur `backend` + `postgres`, volume conservé | oui |
| `scripts/demo/verify_demo_data.sql` | tableau attendu/trouvé/statut + exception SQL si un comptage diffère | lecture seule |

Variables d'environnement communes : `DEMO_DB_CONTAINER`, `DEMO_DB_USER`
(défaut `postgres`), `DEMO_DB_NAME` (défaut `sustainable_farm`).
`demo-up.sh` accepte en plus `DEMO_AUTOSTART` (défaut `1`),
`DEMO_READY_TIMEOUT` (60 s) et `DEMO_BACKEND_TIMEOUT` (240 s).

`demo-reset.sh --dry-run` affiche ce qui serait supprimé sans rien supprimer.

---

## 1. Prérequis — 2 min

```bash
docker info >/dev/null && echo "docker OK"
git branch --show-current          # feature/producttransformation/demo-hardening
ls .env                            # obligatoire pour docker compose
```

Construire l'image backend **depuis cette branche** — l'image actuelle peut
dater d'avant le correctif du KPI :

```bash
docker compose up -d --build postgres backend
docker ps --format '{{.Names}}\t{{.Status}}' | grep sustainable-farm
```

Attendu : `sustainable-farm-db … (healthy)` et `sustainable-farm-backend … (healthy)`.

> Le backend `:8080` lancé depuis une image ancienne renvoie encore `75.00` pour
> le KPI « Grade A » (valeur codée en dur supprimée par `1109099`). Si vous voyez
> `75.00`, l'image n'est pas à jour.

---

## 2. Charger le jeu de démonstration — 2 min

```bash
scripts/demo/demo-up.sh
```

Attendu (extrait) :

```
[demo-up] application du seed : backend/src/main/resources/seed/producttransformation-demo-seed.sql
[demo-up] vérification des comptages (verify_demo_data.sql)
+--------------------+---------+--------+--------+
|       table        | attendu | trouve | statut |
...
| total attendu | total trouve |
|            48 |           48 |
OK : les 11 comptages attendus sont au complet.
[demo-up] OK — jeu de démonstration chargé et vérifié.
```

Relancer la même commande doit donner **exactement** les mêmes 48 lignes :
c'est la preuve d'idempotence (`ON CONFLICT DO NOTHING` dans le seed).

### Si `demo-up.sh` échoue

| Message | Cause | Correctif |
|---|---|---|
| `docker est introuvable dans le PATH` | Docker absent | installer Docker / ouvrir Docker Desktop |
| `le démon Docker ne répond pas` | Docker arrêté | démarrer le démon |
| `Postgres n'est pas démarré et DEMO_AUTOSTART=0` | base arrêtée | `docker compose up -d postgres` |
| `les tables PT sont absentes …` | migrations jamais jouées | `docker compose up -d backend` puis relancer |
| `Jeu de demonstration incomplet -> qc_checkpoint : attendu 6, trouve 5` | une ligne a été supprimée | relancer `demo-up.sh` (il réinsère la ligne manquante) |
| `historical_harvest : attendu 12, trouve 24` | seed chargé il y a plus d'un mois | `demo-reset.sh` puis `demo-up.sh` |

---

## 3. Contrôler l'API — 3 min

```bash
B=http://localhost:8080
curl -s $B/api/product-transformation/dashboard/kpis | jq -c .
```

Attendu sur un jeu de démo **seul** : `gradeAPercentage: 73.25`,
`qualityTargetStatus: "above"`. (Après un golden path qui a ajouté des lots
`LOT-GP-*`, la valeur monte : 83.80 sur la base de répétition.)

Les endpoints des écrans (9) :

```bash
for p in batches raw-intakes wash-sort-records drying-runs qc-checkpoints \
         compliance-records packaging-records equipment operators; do
  printf '%-22s %s\n' "$p" "$(curl -s -o /dev/null -w '%{http_code}' $B/api/$p)"
done
```

Attendu : `200` partout (10 endpoints ; `operators` inclus depuis C2).

---

## 4. Lancer le frontend — 3 min

```bash
cd frontend
npm ci                                # une seule fois
VITE_API_URL=http://localhost:8080 npx vite --port 5173 --strictPort
```

Ouvrir <http://localhost:5173/product-transformation>. **Ctrl+Shift+R** au
premier chargement.

⚠️ Le proxy de `vite.config.js` pointe sur `:8080` ; `VITE_API_URL` est
nécessaire si le backend tourne ailleurs. `app.cors.allowed-origins` autorise
déjà `http://localhost:5173`.

Parcours de contrôle (2 min) :

1. **Dashboard** : KPI « Grade A » chiffré, 5 graphiques alimentés.
2. **Lots** : `New Batch` → modal → créer `LOT-RUNBOOK-01` → la ligne apparaît
   sans rechargement ; recréer le même identifiant → `Batch already exists`
   (409, pas d'écrasement).
3. **Lots** : `Advance` deux fois → `CREATED → INTAKE → WASHING` ; troisième
   clic refusé (QC `WASHING` exigé) — c'est la règle métier.
4. **Operators** : les 2 opérateurs DEMO s'affichent, filtres rôle/statut actifs.
5. **Menu** : 10 entrées, `Settings` absent, bouton `Log out` qui affiche
   « Authentication is not enabled in this demo. ».

---

## 5. Répéter le cycle complet — 3 min

```bash
scripts/demo/demo-reset.sh            # ou --dry-run d'abord
scripts/demo/demo-up.sh
```

Attendu : `OK — base nettoyée` puis les 48 lignes. Les opérateurs/équipements
encore référencés par des lots `LOT-GP-*` sont conservés et signalés — c'est
normal, `demo-up.sh` les réutilise.

Pour repartir d'une base totalement vierge (migrations rejouées) :

```bash
docker compose down -v                # ⚠️ supprime TOUT le volume (tous modules)
docker compose up -d --build postgres backend
scripts/demo/demo-up.sh
```

---

## 6. Séquence de démo (5 min)

1. **Problème** : 20 % de perte post-récolte, traçabilité manuelle.
2. **Dashboard** : KPI Grade A réel (calculé depuis `raw_intake`), tendances.
3. **Lots** : création en direct, puis refus du doublon (garde-fou).
4. **Avancement** : `CREATED → INTAKE → WASHING`, puis refus sans QC `WASHING`.
5. **QC** : créer le contrôle, revenir sur Lots, l'avancement passe.
6. **Séchage / Conditionnement / Conformité** : écrans alimentés.
7. **Ce que le module ne fait pas** : pas d'authentification, données
   synthétiques, modules Plants/Energy non branchés sur cette base.

---

## 7. Fin de session

```bash
scripts/demo/demo-down.sh             # arrête backend + postgres, garde le volume
```

Pour arrêter d'autres services :
`DEMO_SERVICES="frontend backend postgres" scripts/demo/demo-down.sh`.

Frontend : `Ctrl+C` dans le terminal Vite.

---

## 8. Limites assumées

- **Pas d'authentification** : tous les endpoints PT sont ouverts ; le bouton
  `Log out` le dit explicitement au lieu de faire semblant.
- **Données synthétiques** : `LOT-DEMO-*`, `RI-DEMO-*`, `QC-DEMO-*`,
  `EQ-DEMO-*`, `OP-DEMO-*`, plus les 12 mois `historical_harvest` marqués
  `weather_condition = 'DEMO'`.
- **Base de répétition** : les lots `LOT-GP-*` du golden path et les lots créés
  à la main cohabitent avec le seed ; les comptages de vérification ne portent
  que sur les lignes `DEMO`, ils restent donc stables.
- **`demo-down.sh` arrête la pile partagée** (`backend` + `postgres`) : sur un
  poste où d'autres modules tournent, préférer `DEMO_SERVICES=backend`.
- **Emplacement des scripts** : `scripts/demo/` plutôt que `db/demo/` et
  `docs/`, car `db/` (nouvelle entrée racine) et `docs/` sont réservés aux
  admins par `scripts/module-ownership-guard.sh`. Un compte non-admin peut
  modifier `scripts/` sans changer la configuration du dépôt.
