# ADR 0003 : communication entre modules

| Champ | Valeur |
|---|---|
| Statut | Accepted |
| Date | 2026-11-17 |
| Tâches liées | T13, T15 à T25, T47 |
| Critères | AC-8, AC-10 |

## Contexte

Le backend est un monolithe modulaire : une seule application Spring Boot, un seul processus, une seule base PostgreSQL. Chaque module est un package sous `com.infineonbit.sustainablefarm.modules`, le code partagé est sous `com.infineonbit.sustainablefarm.core`. Rien ne vérifie aujourd'hui les frontières :

| Constat | Détail |
|---|---|
| Import direct | watersupply `PlantsGrowthCalendarAdapter` importe `plants.service.GrowthCalendarService`, une classe interne de plants |
| HTTP vers soi-même | producttransformation appelle plants en HTTP sur sa propre application (`RestTemplate`, `plants.api.base-url`) et un faux contrôleur crop-storage en boucle |
| Données recopiées | watersupply reçoit les visiteurs par un endpoint d'entrée `/api/water/visitor-events` au lieu de réagir à visitormanagement |
| Gestionnaire d'erreurs | sitesecurity garde son propre `@ControllerAdvice` (`SiteSecurityExceptionHandler`) |

## Décision

| Besoin | Mécanisme |
|---|---|
| Lecture synchrone d'un autre module | Appel direct d'une interface publique placée dans le package `modules.<x>.api` du module propriétaire. Ce package contient les interfaces et les DTO records exposés, rien d'autre. Les autres packages du module (`service`, `repository`, `entity`, `dto`, `controller`) sont internes |
| Effet de bord dans un autre module | Événement Spring publié par le module source (`ApplicationEventPublisher`) et consommé par `@TransactionalEventListener` dans le module cible, après validation de la transaction source |
| Appel HTTP vers sa propre application | Interdit |
| Dépendance de `core` | `core` ne dépend d'aucun module ; les modules dépendent de `core` |
| Gestion des erreurs | Uniquement dans `core` (`CoreExceptionHandler`, voir ADR 0002) |

Ces règles sont vérifiées par le test ArchUnit `backend/src/test/java/com/infineonbit/sustainablefarm/architecture/ModuleBoundariesTest.java`, joué par `./mvnw test` en CI et par le gate ARC Core :

| Règle | Contenu |
|---|---|
| Frontières | Une classe de `modules.<x>` ne dépend d'un autre module `modules.<y>` qu'à travers `modules.<y>.api` |
| Core | Aucune classe de `core` ne dépend d'une classe de `modules` |
| Erreurs | Aucune classe hors de `core` n'est annotée `@ControllerAdvice` ou `@RestControllerAdvice`, ni nommée `*ExceptionHandler` |

Les violations existantes sont gelées (`FreezingArchRule`, store versionné sous `backend/src/test/resources/archunit_store`) : le test passe aujourd'hui et toute nouvelle violation le fait échouer. Les tâches T15 à T25 suppriment les violations gelées, T47 retire le gel.

## Conséquences

| Type | Conséquence |
|---|---|
| Positive | Un module peut changer ses classes internes sans casser les autres, seul son package `api` est un contrat |
| Positive | Plus d'appel réseau interne : moins de latence, de configuration et de pannes partielles, et les transactions restent locales |
| Positive | Les frontières sont vérifiées automatiquement, pas seulement en revue |
| Négative | Chaque module consommé doit écrire et maintenir son package `api` |
| Négative | Les événements Spring ne sont pas persistés : un listener en échec après validation ne rejoue pas l'effet. Acceptable tant qu'aucun effet n'est critique ; à réévaluer avec Spring Modulith |
| Neutre | Le store de gel doit être mis à jour quand une tâche corrige une violation (voir la javadoc du test) |

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| Appels HTTP entre modules | Rejetée | Coût réseau et sérialisation sans bénéfice dans un seul processus, c'est l'état à corriger dans producttransformation |
| Accès direct aux services et repositories des autres modules | Rejetée | Couple tous les modules à leurs détails internes |
| Spring Modulith (vérification des modules, événements persistés) | Reportée | Bonne cible, mais impose sa structure de packages et une dépendance de plus ; réévaluée après le Lot 2 |
| Test ArchUnit | Retenue | Simple (taille XS), sans impact sur le code de production, vérifié en CI |
| Modules Maven séparés | Rejetée | Restructuration lourde du build pour un gain que le test ArchUnit apporte déjà |
