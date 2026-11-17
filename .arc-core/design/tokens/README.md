# Tokens de la marque

Ce dossier est un formulaire : il contient tous les tokens du core avec leurs valeurs neutres. Modifier les valeurs, **pas les noms**.

- `primitive/` : vos couleurs, police, rayons, espaces, durées. Ajouter une rampe (ex. `color.brand.500`) est permis.
- `semantic/light.json` et `dark.json` : faire pointer chaque nom (ex. `color.accent.default`) vers vos primitifs. Ne pas créer de nom (contrôle C-TOK-06).
- `contrast.json` (optionnel) : paires de contraste supplémentaires propres à la marque.
- Supprimer un fichier = garder la valeur du core.

Vérifier : `arc-design check tokens --brand <marque>` (contraste AA, parité clair/sombre, grille de 4 px, contrat).
