# Architecture Decision Records

Les décisions d'architecture structurantes de sustainable-farm, issues du plan d'intégration et d'uniformisation des modules. Chaque ADR suit le même format : contexte, décision, conséquences, alternatives considérées.

| Numéro | Titre | Statut | Date |
|---|---|---|---|
| [0001](0001-app-shell-et-design-system.md) | Un shell applicatif unique et un design system basé sur machinery | Accepted | 2026-11-17 |
| [0002](0002-conventions-base-de-donnees-et-api.md) | Conventions de base de données et d'API | Accepted | 2026-11-17 |
| [0003](0003-communication-entre-modules.md) | Communication entre modules | Accepted | 2026-11-17 |
| [0004](0004-referentiels-partages.md) | Référentiels partagés | Accepted | 2026-11-17 |

## Ajouter un ADR

| Étape | Action |
|---|---|
| 1 | Créer `NNNN-titre-court.md` avec le numéro suivant |
| 2 | Reprendre les sections : statut, date, contexte, décision, conséquences, alternatives considérées |
| 3 | Ajouter le fichier à ce tableau et à la liste `doc` de `.github/module-owners.conf` |
| 4 | Un ADR accepté n'est plus modifié sur le fond : une nouvelle décision le remplace et passe son statut à Superseded |
