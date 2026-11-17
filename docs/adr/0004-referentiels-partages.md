# ADR 0004 : référentiels partagés

| Champ | Valeur |
|---|---|
| Statut | Accepted |
| Date | 2026-11-17 |
| Tâches liées | T15 à T25, T48 |
| Critères | AC-5, AC-6, AC-7, AC-8, AC-9 |

## Contexte

Les mêmes concepts métier existent plusieurs fois, sans lien entre eux :

| Concept | Copies actuelles |
|---|---|
| Personnes | machinery `OperatorAssignment`, producttransformation `Operator`, visitormanagement `Staff`, watersupply `User`, sitesecurity `CredentialUser`, plus des noms en texte libre |
| Site | watersupply `Farm`, `Field`, `Zone`, `ZoneBlockMapping` ; plants `farmId Integer` et `blockCode` ; producttransformation `farm_id String` |
| Équipement | machinery `equipment` et producttransformation `pt_equipment` |
| Alertes | energysupply `alert`, salesmarketing `alerts`, watersupply `notifications` |
| Variété | entité dans plants, enum dans producttransformation, String dans salesmarketing |
| Lots | 4 identifiants : plants `lot_number`, producttransformation `batch_id` et `lot_code`, salesmarketing `batch_code` |

Conséquence : un opérateur de producttransformation n'existe pas pour machinery, et un lot de récolte ne remonte pas jusqu'à la vente.

## Décision

Chaque concept a un propriétaire unique ; les autres modules le référencent par clé étrangère et le lisent par le package `api` du propriétaire (ADR 0003).

| Concept | Propriétaire | Exposé par | Ce que les autres modules font |
|---|---|---|---|
| Ferme, bloc (code unique), zone | Nouveau module `farm` | `farm.api` | plants, watersupply et producttransformation référencent Farm, Block et Zone ; `zone_block_mapping`, `farmId Integer` et `farm_id String` disparaissent (T15, T16) |
| Personne et rôles par module | Nouveau module `staff` | `staff.api` | machinery, producttransformation, visitormanagement et watersupply référencent `person` ; `Operator` de producttransformation devient une extension de Person avec rôle et certifications (T17 à T19) |
| Alertes et notifications in-app | Nouveau module `notification` | `notification.api` | Une seule table `alert` (module source, type, sévérité, statut, entité liée, dates) ; energysupply, salesmarketing et watersupply y écrivent et la lisent, leurs écrans d'alertes ne changent pas (T25) |
| Équipement | machinery | `machinery.api` | Une seule table `equipment` ; producttransformation garde une extension 1:1 `pt_equipment_spec` (type de transformation, capacité, énergie) ; `pt_equipment` et le doublon `/api/equipment` disparaissent (T20) |
| Variété | plants | `plants.api` | `MangoVariety` de producttransformation et `Product.variety` de salesmarketing deviennent des clés étrangères (T21) |
| Chaîne des lots | Chaque maillon reste dans son module | Les `api` de plants et producttransformation | plants `harvest_record` vers producttransformation `harvest_event`, `batch` vers `packaging_record`, puis salesmarketing `product_batch`, reliés par clés étrangères sans nouveau comportement (T22, T23) |

### Modules exclus (A12)

| Module | État | Traitement |
|---|---|---|
| cropstorage | Aucun code sur develop | Ni codé ni intégré, absent du modèle partagé et de la navigation. Le stub crop-storage de producttransformation garde son comportement, seul son appel HTTP vers lui-même devient un appel direct (A3) |
| sitesecurity | Classé « Ébauche » (Q6) | Code laissé tel quel, retiré de la navigation, aucune table ni API touchée. T43 et T44 restent bloquées ; son gestionnaire d'erreurs reste une exception gelée du test ArchUnit |

## Conséquences

| Type | Conséquence |
|---|---|
| Positive | Une personne, un lieu, un équipement ou une variété est saisi une seule fois et référencé partout |
| Positive | La traçabilité récolte, transformation, vente devient possible par jointure |
| Négative | Trois nouveaux modules (`farm`, `staff`, `notification`) à ajouter à `module-owners.conf` et aux scripts de garde (T48) |
| Négative | Les modules consommateurs dépendent du propriétaire : un changement de son package `api` les touche tous |
| Neutre | Aucune fonctionnalité n'est ajoutée ni retirée (A11) : pas de cloche de notifications, pas de tableau de bord global, pas d'endpoint de traçabilité |

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| Garder les copies et les synchroniser | Rejetée | Synchronisation à écrire et à maintenir, données divergentes |
| Ranger les référentiels dans `core` | Rejetée | `core` est de l'infrastructure technique sans métier, et ne doit dépendre d'aucun module (ADR 0003) |
| Confier ferme et zone à watersupply, personnes à visitormanagement | Rejetée | Des modules métier deviendraient propriétaires de concepts transverses ; des modules de premier rang sont plus clairs (A7) |
| Table d'équipement propre à producttransformation | Rejetée | Doublon de machinery ; l'extension 1:1 garde les attributs propres à la transformation sans dupliquer l'équipement |
