# Module water supply (SWMS)

Gestion intelligente de l'eau de l'exploitation : irrigation pilotee par le besoin reel des
cultures, suivi des sources et des consommations, qualite de l'eau, alertes de secheresse.
C'est le module de reference du programme : les autres modules (machinery, plants) ont la meme
structure et les memes conventions.

## Ou vit le code

| Partie | Emplacement |
|---|---|
| API REST, services, entites | `backend/src/main/java/com/infineonbit/sustainablefarm/modules/watersupply/` |
| Schema de base (migrations V1 -> V10) | `backend/src/main/resources/db/migration/` |
| Interface (vues, hooks, API client) | `frontend/src/features/watersupply/` |
| Capteurs et firmware ESP32 | `iot/` |

Le module est monte sous `/watersupply` dans l'application (`routes.jsx`), comme `/machinery` et
`/plants` : chaque module porte son propre menu et ses propres ecrans.

## Contrat d'API (resume)

- **Fermes / champs / zones** : `GET|POST /api/farms`, `GET|PUT|DELETE /api/farms/{id}`,
  `GET /api/farms/{id}/fields`, `GET|POST /api/fields`, `GET|PUT|DELETE /api/fields/{id}`,
  `GET /api/fields/{id}/zones`, `GET|POST /api/zones`, `GET|PUT|DELETE /api/zones/{id}`.
  Une zone porte sa methode d'irrigation, son coefficient cultural (Kc) et, pour le
  goutte-a-goutte, le nombre de goutteurs et leur debit nominal.
- **Sources d'eau** : `GET|POST /api/water/sources`, `GET|PUT|DELETE /api/water/sources/{id}`,
  `GET /api/water/sources/{id}/level` (capacite, niveau, pourcentage, statut, reservoir de pluie).
- **Consommations** : `GET|POST /api/water/consumption`, `GET|PUT|DELETE /api/water/consumption/{id}`
  (liste en tableau, ou en page des que `page`/`size` est fourni).
- **Qualite** : `GET|POST /api/water/quality`, `GET|PUT|DELETE /api/water/quality/{id}`.
- **Irrigation** : `GET|POST /api/irrigations`, `GET|PUT|DELETE /api/irrigations/{id}`,
  `POST /api/irrigations/{id}/start|stop`, `POST /api/irrigations/{id}/postpone`,
  `POST /api/irrigations/auto-trigger`, `GET /api/irrigation/suggestions`,
  `GET|POST /api/irrigation-logs`, `GET|PUT|DELETE /api/irrigation-logs/{id}`.
- **Colmatage** : `GET /api/water/zones/{zoneId}/flow-check` (diagnostic de debit de la zone).
- **Eau de pluie** : `GET|POST /api/rainwater-harvests`, `GET|PUT|DELETE /api/rainwater-harvests/{id}`,
  `GET /api/rainwater-harvests/coverage`.
- **Maintenance goutte-a-goutte** : `GET|POST /api/drip-maintenance-logs`,
  `GET|PUT|DELETE /api/drip-maintenance-logs/{id}`, `GET /api/drip-maintenance-logs/schedule`
  (calendrier preventif calcule par zone).
- **Quotas** : `GET|POST /api/water/quotas`, `GET /api/water/quotas/usage`, `GET|PUT|DELETE .../{id}`.
- **Tableau de bord** : `GET /api/dashboard/kpis|activities|alerts|water-savings|savings-series|water-balance|leaks`.
- **IA** : `GET /api/ai/recommendations`, `GET /api/ai/drought-prediction`, `POST /api/ai/analyze`.
- **Meteo** : `GET /api/weather/current`, `GET /api/weather/forecast` (coordonnees optionnelles).
- **Notifications** : `GET|POST /api/notifications`, `GET /api/notifications/{id}`,
  `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, `DELETE /api/notifications/{id}`.
- **Rapports** : `GET /api/reports/consumption|irrigation|quality` et `.../csv`.
- **Sante** : `GET /api/health` (verifie reellement la base, 503 si elle est coupee).

## Regles metier (les valeurs sont en clair dans le code)

| Regle | Reference |
|---|---|
| Besoin hydrique d'une zone | `besoin = surface x ET0 x Kc / efficacite` (FAO-56) — `WaterNeedService` |
| Efficacite du systeme | goutte-a-goutte 0,90 · aspersion 0,75 · gravitaire 0,60 |
| Pilotage automatique par humidite du sol | seuils saisonniers **40 / 45 / 50 / 55 %** (pluies, transition, seche fraiche, seche chaude) + garde « dernier arrosage > 24 h » — `IrrigationAutomationService` |
| Report d'irrigation pour cause de pluie | probabilite >= 60 % et pluie >= 5 mm — `IrrigationService` |
| Quota mensuel | alerte a 80 % puis 100 % — `WaterQuotaService` (evaluation limitee a 1×/60 s) |
| Colmatage / fuite du goutte-a-goutte | volume mesure < 90 % (colmatage) ou > 110 % (fuite) du volume attendu — `DripFlowCheckService` |
| Reservoir d'eau de pluie | >= 95 % debordement probable, >= 50 % utiliser la pluie d'abord — `RainwaterTankMonitor` |
| Alerte repetitive | meme titre non lu et moins de 12 h : pas de doublon — `AlertService` |

## Configuration (variables d'environnement)

| Propriete | Defaut | Role |
|---|---|---|
| `app.weather.latitude` / `longitude` | 10.63 / -4.77 (Banfora) | Seule source des coordonnees agro-meteo (ET0, pluie) |
| `app.irrigation.auto-trigger-enabled` | `true` | Active la tache de pilotage automatique |
| `app.irrigation.auto-trigger-interval-ms` | 900000 (15 min) | Periode de la tache |
| `app.quota.check-throttle-seconds` | 60 | Fenetre minimale entre deux evaluations de quota (0 = desactive) |
| `app.rainwater.weekly-nonirrigation-need-liters` | 0 | Besoin non-irrigation declare : sans lui, la recommandation « pluie d'abord » reste muette |
| `app.cors.allowed-origins` | localhost:5173 | Origines autorisees (PATCH inclus) |

## Contrat d'ingestion IoT

`POST /api/iot/telemetry` (objet ou lot, 202, statut par mesure) — types `level`, `flow`,
`quality`, `rain`, `clogging`, `soil`. Deux points a connaitre :

- **`flow` peut porter `zone_id`** : c'est ce lien qui permet de comparer le volume mesure au
  volume attendu du reseau (sans lui, `flow-check` repond `no_measurement`).
- **`level`** met a jour le niveau de la source et declenche les regles du reservoir de pluie,
  en plus du niveau temps reel lu par `/api/water/sources/{id}/level`.

## Tests

```bash
cd backend && ./mvnw test     # 88 tests (dont un test d'integration HTTP) - PostgreSQL requis
cd frontend && npm test       # 38 tests (vues, hooks, branchement, menu)
cd frontend && npm run lint && npm run build
```

Les tests du module declarent eux-memes leur environnement DOM (`// @vitest-environment jsdom`) :
le projet n'a pas de configuration Vitest globale, et les tests de vues fournissent un client
React Query neuf (`components/views/testRender.jsx`).

## Integrer ce module dans un autre (ou l'inverse)

1. Les donnees partageables sont exposees en lecture par des endpoints dedies
   (`/api/water/sources/{id}/level`, `/api/water/zones/{id}/flow-check`, `/api/dashboard/water-balance`)
   plutot que par des acces directs aux tables.
2. Les reponses d'API sont des DTO (`dto/`), jamais les entites JPA.
3. Le schema evolue uniquement par migration versionnee : **annoncer le numero** avant d'ecrire
   la suivante (dossier partage entre modules).
4. Le module ne modifie jamais les fichiers d'un autre module ni `frontend/src/shared/`.

## Decisions ouvertes (a trancher avec l'equipe)

- **Authentification** : hors perimetre du module (aucun endpoint `/api/auth`), elle viendra de la
  plateforme globale. Les alertes automatiques sont rattachees a un compte technique
  (`SystemUsers.IOT_SYSTEM_USER_ID`, sans mot de passe utilisable).
- **Numeros de migration** : `V10` (pilotage automatique + debit par zone) est a annoncer.
- **`shared/hooks/useAuth.js`** : code mort hors module qui importe `getToken`/`setToken` de
  `shared/api/client.js` — ces fonctions n'existent pas ; a corriger en coordination ou supprimer.

