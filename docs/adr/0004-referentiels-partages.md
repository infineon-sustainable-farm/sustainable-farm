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

### Modèle du site (T15)

Le champ (`Field`) de watersupply et le bloc de plants désignent la même parcelle : il n'y a qu'une entité, `Block`.

| Élément | Décision |
|---|---|
| Hiérarchie | `farm` (Farm) vers `block` (Block) vers `zone` (Zone), clés `Long` de `core.entity.BaseEntity`, FK `block.farm_id` et `zone.block_id` |
| Code de bloc | `block.code` (« A », « B2 »), unique sur toutes les fermes, en majuscules ; vide tant que le bloc n'est pas relié à plants |
| `zone_block_mapping` | Supprimée : le bloc d'une zone est `zone.block_id`, son code Plants est `block.code`. Une zone n'arrose plus qu'un bloc, et toutes les zones d'un bloc partagent son code |
| API REST | Le module `farm` sert `/api/farms`, `/api/fields` (les blocs, sous leur nom watersupply), `/api/zones` et `/api/water/zone-block-mappings` aux mêmes chemins et champs JSON ; les identifiants deviennent des nombres. Relier une zone donne son code au bloc de la zone, la délier l'efface (409 si le bloc porte déjà un autre code ou si le code appartient à un autre bloc) |
| Lecture par les autres modules | `farm.api.FarmDirectory` (ferme, bloc par id ou par code, blocs d'une ferme, zones d'un bloc ou d'une ferme) et ses records `FarmDto`, `BlockDto`, `ZoneDto` |
| Clés étrangères entre modules | Colonne `Long` simple dans le module consommateur, contrainte déclarée par un bean `core.schema.CrossModuleForeignKey` vers `FarmDirectory.FARM_TABLE`, `BLOCK_TABLE` ou `ZONE_TABLE` ; une ligne encore référencée ne peut pas être supprimée (409) |
| Attributs d'irrigation | Méthode d'irrigation, coefficient cultural et goutteurs restent sur `Zone` pour ne pas changer le JSON de `/api/zones` ; leur déplacement vers watersupply est à reconsidérer avec T36 |

### Plants et Product Transformation sur le référentiel (T16)

Plants et Product Transformation désignent un bloc par un code saisi (« A », « B2 ») : ce code est celui d'une ligne `block`. Leurs lignes gardent une seule colonne `Long` vers `block` (clé étrangère `core.schema.CrossModuleForeignKey`), le bloc donnant la ferme ; le JSON garde les mêmes champs.

| Élément | Décision |
|---|---|
| Colonnes plants | `block_id` sur `block_variety` (ex-`varietes`), `growth_calendar` (ex-`calendrier_croissance`), `health_inspection`, `health_treatment`, `fertilizer_movement`, `nursery_event` ; `nursery_batch` garde `farm_id` (FK vers `farm`, la ferme de la pépinière, sans bloc) et `planned_block_id`. `id_ferme`, `bloc_parcelle`, `farm_id Integer` et `block_code` disparaissent |
| Colonnes Product Transformation | `batch.block_id`, `harvest_event.block_id`, `raw_intake.source_block_id` (Long, non nuls) ; `farm_id String`, `block_id String`, `source_farm` et `source_block` disparaissent |
| Code inconnu à l'écriture | Le bloc est créé par `farm.api.BlockRegistry.findOrCreateBlock` : nommé d'après son code, 0 ha, sous la ferme demandée, ou sans ferme demandée sous la ferme par défaut (celle du plus petit id, créée sous le nom « Default farm » si le référentiel est vide). Tout code accepté avant reste accepté, aucun écran ne change |
| Ferme demandée par plants | `farmId` doit exister et posséder le bloc, sinon 400 ; une récolte ou une observation sur un bloc d'une autre ferme reste « aucune plantation » (422 comme avant). Le `farmId` des réponses est la ferme du bloc : il n'est plus `null` pour une ligne saisie sans ferme |
| Ferme demandée par Product Transformation | Texte libre : un code existant l'emporte (le texte de ferme est ignoré) ; pour un nouveau code, le texte désigne une ferme par id numérique ou par nom, sinon la ferme par défaut. Jamais de 400 pour ce texte. Les réponses donnent le code du bloc et le nom de sa ferme |
| Lecture et filtres | Un filtre sur un code ou une ferme inconnus renvoie une liste vide ; le code est comparé après passage en majuscules. Plants lit le code et la ferme de ses lignes par `farm.api` (`FarmDirectory`) au premier usage ; un code renommé dans le référentiel apparaît à la lecture suivante |
| Délier une zone | Refusé (409) quand des lignes d'un autre module référencent son bloc (lu depuis les `CrossModuleForeignKey` vers `block`) : le bloc garde son code, sinon plants et Product Transformation perdraient le bloc qu'ils affichent |
| Données de démonstration | `seed/farm-demo-data.sql` (profil dev, joué en premier, `FarmDirectory.DEMO_SEED_ORDER`) crée la ferme 1 et les blocs A, B, C ; la variété Keitt de plants, la zone Water Supply « Zone Goutte A » et les lots de Product Transformation désignent les mêmes lignes |
| Water Supply | Lit la croissance d'un bloc par `plants.api.BlockGrowthLookup` ; l'import gelé de `plants.service.GrowthCalendarService` disparaît |

### Modèle des alertes (T25)

Les trois copies (energysupply `alert`, salesmarketing `alerts`, watersupply `notifications`) deviennent une seule table `alert` du module `notification`. Ses colonnes sont l'union de ce que les trois modules enregistrent ; aucun champ ne reste hors du modèle commun, il n'y a donc pas de table d'extension.

| Colonne | Origine | Décision |
|---|---|---|
| `id`, `created_at`, `updated_at` | `core.entity.BaseEntity` | Clé `Long` ; les identifiants d'alerte deviennent des nombres dans le JSON des trois modules |
| `source_module` | Nouveau | Enum `SourceModule` (`ENERGYSUPPLY`, `SALESMARKETING`, `WATERSUPPLY`) ; chaque méthode de `AlertBoard` est limitée au module appelant, un id d'un autre module est inconnu (404) |
| `code` | sales `alert_code` | Code métier unique, vide pour les autres modules |
| `type` | sales `alert_type`, watersupply `type` | Texte libre du module |
| `severity` | energysupply et sales | Enum `CRITICAL`, `WARNING`, `INFO` ; sales garde ses valeurs en minuscules dans le JSON ; watersupply la déduit de son `type` quand c'est un niveau |
| `status` | energysupply et sales | Enum `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`, obligatoire, `OPEN` par défaut ; energysupply n'expose que `OPEN` et `RESOLVED` |
| `title`, `message` | watersupply, tous | Message jusqu'à 1000 caractères |
| `related_entity_type`, `related_entity_id` | sales `related_table`, `related_id` ; energysupply `component_id` | Référence polymorphe sans clé étrangère, id en texte ; energysupply y range l'id Long du composant (T34) et renvoie son code (`ENG-002`) dans le JSON, et refuse la suppression d'un composant visé par une alerte (409), comme l'ancienne clé étrangère |
| `region`, `origin` | sales | Facultatifs |
| `recipient_person_id` | watersupply `user_id` | Clé étrangère vers `person` (`CrossModuleForeignKey`) |
| `is_read`, `action_url` | watersupply | Lu ou non, route à ouvrir |
| `raised_at` | energysupply `timestamp`, sales et watersupply `created_at` | Instant de l'alerte, distinct de l'audit `created_at` ; les modules qui exposaient un `LocalDateTime` le convertissent dans le fuseau du serveur |
| `resolved_at` | sales | Posé par sales tel que saisi, et par `changeStatus(RESOLVED)` |

`notification.api.AlertBoard` : `raise`, `raiseOnce` (fenêtre anti-doublon : alerte non lue du même module et du même titre), `find`, `list` (par statut), `page` (par destinataire), `update`, `changeStatus`, `markRead`, `markAllRead`, `delete`, `count`, `countByType`, `isAbout`. Le service de courriel `core.notification.NotificationService` reste un concept distinct.

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
