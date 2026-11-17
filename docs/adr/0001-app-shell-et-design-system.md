# ADR 0001 : un shell applicatif unique et un design system basé sur machinery

| Champ | Valeur |
|---|---|
| Statut | Accepted |
| Date | 2026-11-17 |
| Tâches liées | T7 à T12, T50, puis T27 à T45 |
| Critères | AC-1, AC-2, AC-3, AC-4 |

## Contexte

Les modules frontend ont été construits séparément sous `frontend/src/features/<module>`. Le résultat ressemble à plusieurs applications collées :

| Constat | Détail |
|---|---|
| Shells | 8 sidebars et layouts distincts, un `RootLayout` vide, pas de route `/` ni de page 404 |
| Couleurs | 4 palettes mêlées dans `index.css` (commune, FenceLine, Sales Material, espacements nommés) |
| Icônes | 2 jeux d'icônes (lucide et Material Symbols) |
| Styles | Tailwind et styles inline `ws-*` mélangés dans `shared/`, 949 lignes de CSS dans watersupply, thème sombre dans sitesecurity |
| Navigation | energysupply et sitesecurity naviguent par state, sans URL par écran |
| Textes | tous en dur dans le JSX, en anglais |

Les règles ARC Core (`get_design_brief`, `arc-design check`) imposent des tokens partout, un seul jeu d'icônes à une épaisseur, l'i18n de tout texte, un focus visible, des cibles de 44 px et un texte de 12 px minimum. machinery est le module dont le rendu est retenu comme référence (Q5), avec l'accessibilité reprise de plants.

## Décision

| Sujet | Décision |
|---|---|
| Tokens | Un seul jeu de tokens dans `frontend/src/index.css`, initialisé par `arc-core design init` avec la palette machinery : primitifs et sémantiques (surface, texte, bordure, primaire, succès, alerte, erreur, info), espacement sur 4 px, 6 à 8 tailles de texte, 3 graisses, 4 rayons, 2 ombres, durées, z-index, palette de graphiques. Les anciens blocs restent sous un nom `legacy` jusqu'à T45 |
| Kit | Un kit `frontend/src/shared/ui` au look machinery : Button, IconButton, Card, KpiCard, PageHeader, Field, Input, Select, Textarea, Badge, StatusBadge, Modal accessible (Escape, `role="dialog"`, focus piégé), ConfirmDialog, Table, Pagination, Tabs, EmptyState, ErrorState, Skeleton, Toast. Aucun module ne garde ses propres versions |
| Shell | Un seul `app/AppLayout` : une sidebar globale au style machinery (w-60, bg-primary, lucide 18), un groupe par module construit depuis le `nav` exporté par chaque `routes.jsx`, sous-liens du module actif dépliés, drawer avec overlay sous `md`, en-tête de page commun, page 404 et `errorElement` |
| Lanceur | `/` est une page plein écran sans sidebar : logo, sélecteur de langue, une tuile par module intégré (carré arrondi couleur primaire, icône lucide blanche, nom traduit dessous). Le logo de la sidebar ramène à `/` (A4, Q7) |
| Routes | Kebab-case : `/energy-supply`, `/machinery`, `/plants`, `/product-transformation`, `/sales-marketing`, `/visitor-management`, `/water-supply`. Chaque écran a sa propre URL |
| Icônes | lucide uniquement, une seule épaisseur de trait |
| Thème | Clair partout, le thème sombre FenceLine disparaît (A1) |
| i18n | react-i18next en anglais, allemand et français (Q3). Fichiers rangés par module et par langue (`src/locales/<en,de,fr>/<module>.json`, A13), un script vérifie la parité des clés, une règle ESLint refuse le texte en dur dans le JSX des dossiers migrés. La langue choisie est mémorisée |
| Périmètre | sitesecurity et cropstorage restent hors navigation et hors lanceur (A12) |

## Conséquences

| Type | Conséquence |
|---|---|
| Positive | Une seule apparence et une seule navigation pour l'utilisateur, quel que soit le module |
| Positive | `arc-design check` peut vérifier tout le frontend avec les mêmes règles |
| Positive | Bundle réduit : framer-motion, Material Symbols et 3 polices sont retirés |
| Positive | Les fichiers de traduction par module évitent les conflits entre worktrees parallèles |
| Négative | Chaque module doit être migré écran par écran (T27 à T42), ce qui gèle ses nouvelles features pendant sa migration |
| Négative | Le bloc `legacy` coexiste avec les nouveaux tokens jusqu'à T45 |
| Neutre | `shared/`, `app/` et `index.css` restent réservés à l'admin (`module-owners.conf`) |

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| Shell et sidebar communs, chaque module garde son style | Rejetée | Ne traite que la sidebar, pas la cohérence visuelle |
| Réécriture complète du frontend sur une branche longue | Rejetée | Conflits avec 9 propriétaires, rien de livrable avant la fin |
| Prendre le look de plants comme référence | Rejetée | machinery est la référence visuelle retenue (Q5) ; seules les implémentations accessibles de plants sont reprises |
| Une bibliothèque de composants externe (shadcn, MUI) | Rejetée | Ajoute une dépendance et un style étranger ; les classes shadcn d'energysupply ne sont même pas générées |
| Redirection de `/` vers le tableau de bord machinery | Rejetée | Choix de l'utilisateur : une page lanceur qui ne contient que de la navigation (Q7) |
