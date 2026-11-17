# ADR 0002 : conventions de base de données et d'API

| Champ | Valeur |
|---|---|
| Statut | Accepted |
| Date | 2026-11-17 |
| Tâches liées | T4, T14, T26, T29, T31, T33, T34, T36, T39, T40, T46, T47 |
| Critères | AC-11, AC-12 |

## Contexte

Chaque module a choisi ses propres conventions :

| Sujet | État constaté |
|---|---|
| Clés primaires | Long (machinery, plants, visitormanagement), UUID (watersupply), Integer (salesmarketing), String assignée (producttransformation, sitesecurity, energysupply) |
| Tables | Au singulier, au pluriel, préfixées ou en français (`varietes`, `calendrier_croissance`) |
| Statuts | En String dans salesmarketing, watersupply, sitesecurity |
| Migrations | V1 à V15 sans V3 ni V4, V2, V7 et V10 non idempotentes, 44 tables créées uniquement par `ddl-auto=update`, CI qui ne joue jamais Flyway |
| URL d'API | `/api/<module>`, `/api/v1/...` et `/api/...` sans préfixe mélangés |
| Erreurs | 5 formats d'erreur, ramenés à un seul par T4 (`core.ApiError` servi par `CoreExceptionHandler`) |
| Réponses | salesmarketing renvoie des entités JPA |

Les bases de données existantes sont jetables : il n'y a aucun environnement de production, les données sont des seeds (Q1).

## Décision

### Base de données (A8)

| Sujet | Convention |
|---|---|
| Clé primaire | `Long` en `GenerationType.IDENTITY` partout (Q2), plus un code métier unique lisible quand il existe (numéro de lot, code de bloc, code équipement) |
| Entité de base | Les entités étendent `core.entity.BaseEntity` : `id`, `created_at` et `updated_at` en `Instant`, renseignés par `@PrePersist` et `@PreUpdate` |
| Noms de tables | snake_case, au singulier, en anglais ; préfixe du module seulement en cas d'ambiguïté (par exemple `pt_equipment_spec`) |
| Noms de colonnes | snake_case en anglais, clés étrangères en `<entité>_id` |
| Enums | Statuts et types en enums Java, stockés avec `@Enumerated(EnumType.STRING)` |
| Montants | `BigDecimal` en `numeric(14,2)` accompagné d'une colonne `currency` |
| Quantités | `BigDecimal`, jamais de flottant |
| Relations | Clés étrangères réelles vers le propriétaire du concept (voir ADR 0004), plus d'identifiants bruts en Integer ou String |
| Migrations | Une baseline unique `V1__baseline.sql` écrite depuis le modèle consolidé à la fin du Lot 3 (T46), V1 à V15 supprimées, `spring.jpa.hibernate.ddl-auto=validate`. D'ici là, `ddl-auto=update` permet à l'application de tourner à chaque étape |
| CI | Un test d'intégration rejoue Flyway et valide le schéma sur PostgreSQL 17 à chaque PR (T47) |

### Montants et devises (T14)

Le support monétaire commun est dans `core.money` :

| Élément | Rôle |
|---|---|
| `CurrencyCode` | Enum des devises (`XOF`, `EUR`), stockée par son nom ; connaît son unité mineure (0 décimale pour le franc, 2 pour l'euro) |
| `Money` | Record `@Embeddable` : `amount` en `BigDecimal` toujours à l'échelle 2, arrondi au plus proche avec demi vers le haut (`HALF_UP`), et `currency`. Égalité par devise et valeur ; additionner ou soustraire deux devises différentes lève une `IllegalArgumentException` |
| `CurrencyConversion` | Port de conversion `convert(Money, CurrencyCode, LocalDate)` ; résultat arrondi à l'unité mineure de la devise cible ; `ExchangeRateNotFoundException` (404) si aucun taux n'est enregistré |

Les taux de change restent la propriété de plants (table `currency_rate`, saisie par `PUT /api/plants/currency-rates/EUR/XOF`, chargement par défaut en profil `dev`). Plants implémente le port avec le bean `CurrencyRateConversion` : un autre module injecte `CurrencyConversion` et ne dépend que de `core`. La table ne garde pas d'historique : la date est acceptée pour l'avenir, le taux enregistré vaut pour toute date.

Un module déclare un champ monétaire ainsi, avec les colonnes `<prefix>_amount numeric(14,2)` et `<prefix>_currency varchar(3)` :

```java
@Embedded
@AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 14, scale = 2))
@AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3))
private Money price;
```

Les deux colonnes vides donnent un champ `null`. Les colonnes existantes sans devise (machinery, visitormanagement) ou avec la devise dans le nom (`*_eur` d'energysupply et salesmarketing) passent à ce modèle module par module au Lot 3.

### API (A5)

| Sujet | Convention |
|---|---|
| URL | `/api/<module-kebab>/<ressource>`, par exemple `/api/water-supply/zones`, `/api/product-transformation/batches`. Pas de `/api/v1`, pas de ressource sans préfixe de module |
| Ressources | Noms au pluriel en kebab-case |
| Corps | DTO en `record` Java, jamais d'entité JPA en entrée ni en sortie |
| Pagination | `core.web.PageResponse` |
| Erreurs | Un seul format, `core.exception.ApiError` (`timestamp`, `status`, `code`, `error`, `message`, `path`, `fieldErrors`), produit uniquement par `core.exception.CoreExceptionHandler` |
| Exceptions | Les modules lèvent les exceptions de `core.exception` (`ResourceNotFoundException`, `ConflictException`, `BadRequestException`, `BusinessRuleException`) ; aucun module n'a son propre `@RestControllerAdvice` |

## Conséquences

| Type | Conséquence |
|---|---|
| Positive | Un schéma lisible et homogène, que Hibernate valide au lieu de le créer |
| Positive | Les clés étrangères entre modules deviennent possibles, car toutes les PK ont le même type |
| Positive | Le frontend traite toutes les erreurs avec un seul parseur |
| Négative | Toutes les URL d'API changent : backend et frontend d'un module changent dans la même PR |
| Négative | Les données existantes sont perdues ; elles sont rechargées depuis les seeds du profil `dev` |
| Neutre | Les conversions UUID et String vers Long ne demandent aucune migration de données, grâce à la baseline unique |

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| UUID partout | Rejetée | Seul watersupply l'utilise ; Long IDENTITY est déjà utilisé par 36 entités sur 100 (Q2) |
| Garder V1 à V15 et ajouter des migrations de conversion | Rejetée | Coûteux (UUID et String vers Long) et inutile puisque les bases sont jetables (Q1) |
| Versionner l'API (`/api/v1`) | Rejetée | Un seul client, livré avec le backend ; le versionnement n'apporte rien aujourd'hui |
| Un gestionnaire d'erreurs par module | Rejetée | C'est l'état qui a produit 5 formats d'erreur |
| Exposer les entités directement | Rejetée | Couple le contrat d'API au schéma et expose les relations paresseuses |
