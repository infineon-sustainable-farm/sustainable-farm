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
| Shell | Un seul `app/AppLayout` et un seul composant de sidebar au style machinery (w-60, bg-primary, lucide 18). Chaque module a sa propre sidebar : elle ne montre que ce module (son nom sous le logo, ses liens et sections construits depuis le `nav` de son `routes.jsx`, son `sidebarFooter`), plus un lien « Tous les modules » vers le lanceur ; on change de module depuis le lanceur. Drawer avec overlay sous `md`, en-tête de page commun, page 404 et `errorElement` |
| Lanceur | `/` est une page plein écran sans sidebar : logo, sélecteur de langue, une tuile par module intégré (carré arrondi couleur primaire, icône lucide blanche, nom traduit dessous). Le logo de la sidebar ramène à `/` (A4, Q7) |
| Routes | Kebab-case pour les nouveaux chemins : `/energy-supply`, `/machinery`, `/plants`, `/product-transformation`. sales, visitor et water gardent leur URL d'origine (`/salesmarketing`, `/visitormanagement`, `/watersupply`, voir Mise en œuvre). Chaque écran a sa propre URL |
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

## Mise en œuvre

État du code à la fin de l'intégration (T45, T53), là où il précise ou corrige la décision ci-dessus :

| Sujet | État final |
|---|---|
| Routes | `/sales-marketing`, `/visitor-management` et `/water-supply` n'ont pas été adoptés : la migration de leurs écrans a gardé les mêmes entrées, le même ordre et les mêmes cibles que leur ancienne sidebar, donc `/salesmarketing`, `/visitormanagement` et `/watersupply`. Les renommer reste possible : chemin de la route et `basePath` du `nav` dans `routes.jsx` (`paths.js` pour watersupply). Les chemins d'API, eux, suivent l'ADR 0002 (`/api/sales-marketing`, `/api/visitor-management`, `/api/water-supply`) |
| Tokens | Le bloc `legacy` est supprimé (T45). `index.css` garde un seul bloc séparé, les tokens FenceLine de sitesecurity, module exclu ; rien d'autre ne doit les utiliser |
| Contrat `nav` | En plus de `id`, `basePath`, `icon`, `nameKey`, `items` et `shell`, un lien peut porter un `sectionKey` (intertitre de la sidebar, utilisé par watersupply) et un module un `sidebarFooter` (composant en bas de la sidebar quand le module est courant, utilisé par machinery pour les stocks bas) (T53) |
| Toasts | Le fournisseur de toasts est monté une seule fois dans `app/RootLayout.jsx` (T53) |
| i18n | La règle ESLint anti-texte-en-dur couvre tout `src`, sauf les tests et les deux modules exclus (T45). L'anglais est embarqué ; l'allemand et le français sont chargés à la demande, un fichier par module et par langue. La langue est mémorisée sous la clé `sf.lang` |
| Écrans | Chaque écran de module est chargé à sa première visite (`shared/utils/lazyScreen`) |
| Marque | Les tokens et la marque d'ARC Core sont dans `.arc-core/design` ; `scripts/design-check.sh` et `arc-design check tokens` sont joués par le gate |

La description complète est dans [ARCHITECTURE.md](../ARCHITECTURE.md#3-frontend).
