# Kit `shared/ui`

Le kit de composants commun à tous les modules du frontend. Il reprend le rendu du module machinery
(décision Q5, [ADR 0001](../../../../docs/adr/0001-app-shell-et-design-system.md)) et n'y ajoute que
l'accessibilité et les états qui manquaient : focus visible au clavier, cibles tactiles, sémantique
ARIA, état chargement, état désactivé, mouvement réduit.

| Règle | Application |
|---|---|
| Import | Toujours depuis `src/shared/ui` (fichier `index.js`), jamais un fichier du kit directement |
| Valeurs visuelles | Uniquement les tokens de la section principale de [`src/index.css`](../../index.css) ; jamais de hex, de px, de ms ni de z-index en dur ; jamais les tokens du bloc sitesecurity (module exclu, non migré) |
| Textes | Aucun texte en dur. Les textes par défaut (Fermer, Annuler, Supprimer, Réessayer, Aucun résultat, Chargement, pagination) viennent de l'espace de noms `common` ; les textes métier sont passés en props, déjà traduits avec `t()` |
| Focus | Contour de 2 px couleur primaire sur `:focus-visible` uniquement, jamais au clic |
| Cibles tactiles | Taille machinery sur ordinateur, 44 px sur écran tactile (`pointer-coarse:`) |
| Mouvement | Transitions des tokens (`--duration-fast` 150 ms, `--duration-base` 200 ms, `--duration-slow` 300 ms), coupées sous `prefers-reduced-motion` |
| État | Jamais porté par la couleur seule : un mot ou une icône l'accompagne |
| Vérification | `npm run lint` (texte en dur, parité des langues), `npm test -- shared/ui` |

Les anciens composants de `shared/components` ont été supprimés en T45 : tous les modules intégrés
utilisent ce kit.

## Correspondance des classes machinery

| Rendu machinery | Token du kit |
|---|---|
| `bg-white` (carte, dialogue, champ) | `bg-surface-raised` |
| `bg-gray-50` (survol, fond discret) | `bg-surface-muted` |
| `bg-gray-100` (blocs de chargement, badge neutre) | `bg-neutral-bg` |
| `text-gray-600` (badge neutre) | `text-neutral-fg` |
| `text-gray-500` (sous-titres, en-têtes de tableau) | `text-text-muted` |
| `text-gray-400` (indications, dates) | `text-text-muted` (gray-400 ne passe pas AA) |
| `text-gray-700` (libellé des tuiles KPI) | `text-text` |
| `border-gray-100` (cartes, lignes de tableau) | `border-border-subtle` |
| `border-gray-200` (en-tête de tableau, pagination) | `border-border-divider` |
| `border-gray-300` (champs, bouton secondaire) | `border-border-input` |
| `bg-primary text-white` | `bg-primary text-on-primary` |
| `text-primary` (texte et icônes d'action) | `text-text-accent` |
| `bg-red-600 text-white` (supprimer) | `bg-danger text-on-primary` |
| `text-red-600` (icône supprimer, erreur de champ) | `text-error-fg` |
| `bg-red-50 text-red-700` (erreur serveur) | `bg-error-tint text-error-fg` |
| `bg-green-100 text-green-700`, `amber`, `red`, `sky` | `bg-success-bg text-success-fg`, `warning`, `error`, `info` |
| `bg-teal-50 text-primary` (tuile KPI) | `bg-primary-tint text-primary` |
| `bg-amber-50 text-amber-600`, `bg-red-50 text-red-600`, `bg-sky-50 text-sky-700` | `bg-warning-tint text-warning-fg`, `bg-error-tint text-error-fg`, `bg-info-tint text-info-fg` |
| `bg-black/50` (fond des dialogues) | `bg-backdrop` |
| `rounded-lg` | `rounded-control` |
| `rounded-xl` | `rounded-tile` |
| `rounded-2xl` | `rounded-card` |
| `rounded-full` | `rounded-pill` |
| `shadow-sm`, `shadow-md` | `shadow-raised`, `shadow-overlay` |
| `z-50` | `z-(--z-overlay)` ; toasts `z-(--z-toast)` |
| `transition-all` | `transition duration-(--duration-fast) ease-standard` |

## Composants

### Button

Bouton d'action. Rendu machinery : `bg-primary text-white px-4 py-2 rounded-lg`, 40 px de haut.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `variant` | `primary`, `secondary`, `danger`, `ghost` | `primary` | Primaire plein, secondaire bordé gris, danger rouge, ghost texte primaire |
| `size` | `sm`, `md`, `lg` | `md` | 36, 40 et 44 px ; 44 px sur écran tactile |
| `icon` | composant lucide | | Icône avant le libellé, décorative |
| `loading` | booléen | `false` | Spinner, `aria-busy`, clic bloqué |
| `disabled` | booléen | `false` | Opacité 60 %, curseur interdit |
| `fullWidth` | booléen | `false` | Pleine largeur ; seul cas où le libellé peut passer à la ligne |
| `type` | `button`, `submit`, `reset` | `button` | Type natif |

Le survol garde la couleur d'un bouton plein, l'appui l'assombrit (`primary-pressed`, `danger-pressed`)
et le réduit à 0,98. Le libellé reste sur une ligne (`whitespace-nowrap`) : ce sont les rangées de
boutons (en-tête de page, pied de dialogue, pagination) qui passent à la ligne (`flex-wrap`). Un
bouton `fullWidth` prend la largeur de son conteneur, son libellé peut donc y passer à la ligne.
Les libellés allemands et français restent courts (environ 20 caractères) pour tenir.

### IconButton

Bouton fait d'une seule icône (actions des lignes de tableau). Machinery : icône de 16 px sans
zone de clic ; le kit donne une cible de 32 px (44 px au toucher) et une surface au survol.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `icon` | composant lucide | requis | Icône |
| `label` | chaîne traduite | requis | Nom accessible (`aria-label`) et infobulle ; sans lui le composant lève une erreur |
| `tone` | `primary`, `danger`, `neutral` | `primary` | Couleur de l'icône |
| `size` | `sm`, `md` | `sm` | 32 ou 40 px |

### Card

Surface qui regroupe un contenu. Machinery : `bg-white rounded-2xl border border-gray-100 shadow-sm p-5`.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `title` | nœud | | Titre `text-lg font-semibold` |
| `subtitle` | nœud | | Sous-titre `text-sm` gris |
| `actions` | nœud | | Boutons à droite du titre |
| `titleAs` | `h2`, `h3` | `h2` | Niveau du titre |
| `padding` | `md`, `none` | `md` | `none` pour un tableau qui remplit la carte |
| `as` | élément | `section` | Élément rendu |

### KpiCard

Tuile de chiffre clé du tableau de bord machinery (`DashboardKpiCard`).

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `icon` | composant lucide | requis | Icône de la tuile de 40 px |
| `label` | nœud | requis | Libellé |
| `value` | nœud | requis | Valeur `text-3xl font-bold` |
| `hint` | nœud | | Précision `text-xs` |
| `tone` | `primary`, `warning`, `error`, `info` | `primary` | Teinte de la tuile |
| `to` | chaîne | | Route ouverte par la carte, qui devient un lien |

### PageHeader

En-tête d'écran, comme le tableau de bord machinery.

| Prop | Type | Rôle |
|---|---|---|
| `title` | nœud | Titre `h1` `text-2xl font-bold` |
| `subtitle` | nœud | Sous-titre `text-sm` gris |
| `meta` | nœud | Texte discret à droite (date) |
| `actions` | nœud | Bouton principal à droite, passe dessous sur petit écran |

### Field, Input, Select, Textarea

`Field` pose le libellé, l'aide et l'erreur d'un champ et relie le tout au contrôle qu'il entoure :
`id`, `aria-describedby`, `aria-invalid`, `required`, `disabled`. L'erreur affiche une icône et un
texte. Rendu machinery : libellé `text-sm font-medium`, contrôle à 4 px, erreur `text-xs` rouge.

| Prop de Field | Type | Rôle |
|---|---|---|
| `label` | nœud | Libellé |
| `hint` | nœud | Aide, affichée s'il n'y a pas d'erreur |
| `error` | nœud | Message d'erreur, marque le contrôle invalide |
| `id` | chaîne | Id du contrôle, généré sinon |
| `required`, `disabled` | booléen | Transmis au contrôle |

`Input`, `Select` et `Textarea` acceptent les props natives et une `ref`. Rendu machinery :
`border border-gray-300 rounded-lg w-full px-3 py-2`. `Select` prend des `<option>` ou
`options` (`[{ value, label, disabled }]`) ; `Textarea` a 3 lignes par défaut.

La bordure `border-input` (gray-300) donne 1,5:1 sur blanc, sous les 3:1 demandés pour un bord de
champ. Elle est gardée pour le rendu machinery ; le propriétaire tranche à la démo (T50).

### OwnedResourceSelect, OwnerHint

Choix d'une ressource dont un autre module est propriétaire (équipement, personne, ferme, parcelle,
zone, variété, récolte, lot) : un `Field` et un `Select` du kit, remplis par l'appelant depuis l'API
du module propriétaire. Le composant ne charge rien : l'écran garde sa façon de charger (React Query
ou `useEffect`) et passe `options`, `loading`, `loadError` et `onRetry`.

Quand la liste est vide, le champ est désactivé (« Aucun choix disponible ») et un renvoi s'affiche
dessous : icône, « Demandez au module {{module}} de l'ajouter. » et un lien « L'ajouter dans
{{module}} » qui ouvre le formulaire de création du propriétaire. Le nom du module est sa clé
`<module>:name`, donc traduit. Le renvoi est relié au champ par `aria-describedby`.

```jsx
import { OwnedResourceSelect } from '../../../shared/ui'
import { OWNER_LINKS } from '../../../app/ownerLinks'

<OwnedResourceSelect
  label={t('drying.machine')}
  value={form.equipmentId}
  onChange={(id) => setForm({ ...form, equipmentId: id })}
  options={machines.data ?? []}
  loading={machines.isPending}
  loadError={machines.isError}
  onRetry={machines.refetch}
  owner={OWNER_LINKS.equipment}
  required
/>
```

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `label` | nœud | requis | Libellé |
| `value` | chaîne, nombre | | Id de l'option choisie, `''` ou `null` pour aucune |
| `onChange` | `(id, option) => void` | | Id choisi (chaîne) et l'objet de l'option, `null` pour aucune |
| `options` | tableau | `[]` | Ressources chargées depuis l'API du propriétaire |
| `getOptionValue` | fonction | `option.id` | Valeur d'une option |
| `getOptionLabel` | fonction | `option.name` | Libellé d'une option, déjà traduit s'il le faut |
| `placeholder` | chaîne | `common:owner.choose` | Première option vide |
| `loading` | booléen | `false` | Champ désactivé, `aria-busy`, option « Chargement… » |
| `loadError` | nœud ou `true` | | Échec du chargement : message en alerte avec icône (`common:owner.loadError` pour `true`) |
| `onRetry` | fonction | | Bouton Réessayer sous l'échec |
| `owner` | `{ moduleId, createPath }` | | Module propriétaire, pris dans `app/ownerLinks.js` |
| `showOwnerLinkAlways` | booléen | `false` | Renvoi discret sous une liste remplie : « Absent de la liste ? Demandez au module {{module}}. » |
| `hint`, `error`, `required`, `disabled`, `id`, `name` | | | Comme `Field` et `Select` |

`OwnerHint` n'affiche que la ligne de renvoi, là où il n'y a pas de liste (props `owner`, `variant`
`empty` ou `more`, `id` pour `aria-describedby`). Le lien fait 44 px au toucher.

Lien vers le formulaire de création : `shared/hooks/useCreateParam.js`. Le propriétaire lit
`?create=1` (ou une valeur par dialogue, `?create=field`) avec `useCreateParam(value)`, qui renvoie
`[open, clear]` : le dialogue est ouvert tant que `open` est vrai, et `clear()` retire le paramètre
à sa fermeture. `ownerCreatePath(chemin, value)` construit l'URL. Les liens de chaque ressource
sont dans `app/ownerLinks.js`.

### Badge, StatusBadge

`Badge` : petite étiquette `rounded-full text-xs font-semibold px-2 py-0.5` (DueBadge machinery).

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `tone` | `success`, `warning`, `error`, `info`, `neutral` | `neutral` | Couleurs |
| `icon` | composant lucide | | Icône de 12 px avant le texte |
| `label` | chaîne traduite | | Nom accessible d'un badge sans texte |
| `size` | `sm`, `md` | `sm` | `py-0.5` ou `py-1` |

`StatusBadge` : état d'un enregistrement, comme le StatusBadge machinery (`px-2 py-1`). Props `tone`,
`label` (texte traduit, toujours affiché), `withIcon` (icône du ton), `icon` (icône choisie).

### Modal

Dialogue accessible. Rendu machinery : fond noir à 50 %, panneau blanc `rounded-lg p-6 max-w-md`,
titre `text-lg font-bold`. Le kit ajoute `role="dialog"`, `aria-modal`, le titre et la description
reliés, Escape, le clic sur le fond, le focus piégé dans le dialogue et rendu à l'élément d'origine,
un bouton Fermer, le blocage du défilement de la page et une entrée de 300 ms.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `open` | booléen | requis | Ouvert ou fermé |
| `onClose` | fonction | requis | Escape, fond, bouton Fermer |
| `title` | nœud | requis | Titre, nomme le dialogue |
| `description` | nœud | | Texte sous le titre, décrit le dialogue |
| `footer` | nœud | | Boutons, alignés à droite |
| `size` | `sm`, `md`, `lg`, `xl` | `md` | Largeur maximale |
| `initialFocusRef` | ref | | Élément focalisé à l'ouverture (premier champ sinon) |
| `dismissible` | booléen | `true` | `false` bloque Escape et le fond pendant une requête |
| `closeLabel` | chaîne | `common:actions.close` | Nom du bouton Fermer |

### ConfirmDialog

Confirmation avant une action destructrice, sur `Modal`, comme les dialogues de suppression
machinery. Le focus part sur Annuler.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `open`, `onClose`, `onConfirm` | | requis | Ouverture et réponses |
| `message` | nœud | requis | Ce qui est supprimé |
| `title` | nœud | `common:confirm.deleteTitle` | Titre |
| `confirmLabel` | chaîne | `common:actions.delete` | Bouton de confirmation |
| `cancelLabel` | chaîne | `common:actions.cancel` | Bouton d'annulation |
| `variant` | `danger`, `primary` | `danger` | Style du bouton de confirmation |
| `loading` | booléen | `false` | Spinner, dialogue non fermable |
| `error` | nœud | | Échec de la requête, en alerte avec icône |

### Table

Tableau de données au rendu machinery : `w-full text-left`, en-tête `border-b border-gray-200
text-sm text-gray-500`, lignes `border-b border-gray-100`, cellules `py-2`, première colonne en
`font-medium`. Le débordement horizontal défile dans le tableau, pas dans la page.

| Prop | Type | Défaut | Rôle |
|---|---|---|---|
| `columns` | `[{ key, header, cell, sortable, sortValue, align, className }]` | requis | Colonnes ; `cell(row)` rend la cellule, `row[key]` sinon |
| `rows` | tableau | `[]` | Données |
| `getRowKey` | fonction | `row.id` | Clé de ligne |
| `caption` | nœud | | Nom accessible du tableau, masqué à l'écran |
| `rowActions` | `(row) => nœud` | | Dernière colonne (IconButton) |
| `actionsHeader` | chaîne | `common:table.actions` | En-tête de la colonne d'actions |
| `loading` | booléen | `false` | Lignes squelettes, `aria-busy`, annonce Chargement |
| `skeletonRows` | nombre | `5` | Nombre de lignes squelettes |
| `emptyMessage` | nœud | `common:status.noResults` | Ligne affichée sans données |
| `defaultSort` | `{ key, direction }` | | Tri local initial |
| `sort`, `onSortChange` | | | Tri contrôlé (côté serveur) : les lignes sont affichées telles quelles |

Un en-tête triable est un bouton avec `aria-sort` (`ascending`, `descending`, `none`) et une icône
de sens.

### Pagination

Rendu de l’ancienne pagination machinery : à droite, Page x sur y, puis Précédent et Suivant
`px-3 py-1 rounded-lg border border-gray-200`. Le kit ajoute un repère de navigation nommé, le focus
et 44 px au toucher. Pages numérotées à partir de 0, comme l'API.

| Prop | Type | Rôle |
|---|---|---|
| `page` | nombre | Page courante, à partir de 0 |
| `totalPages` | nombre | Nombre de pages ; rien n'est rendu à 0 |
| `onPageChange` | fonction | Reçoit la nouvelle page |
| `label` | chaîne | Nom de la navigation, `common:pagination.label` par défaut |

### Tabs

Absent de machinery, construit sur ses tokens : rail gris clair, onglet choisi sur surface blanche
avec ombre et en gras, sans trait de soulignement. Modèle d'onglets WAI-ARIA : un seul arrêt de
tabulation, flèches gauche et droite, Début et Fin, onglets désactivés sautés. Chaque libellé reste
sur une ligne ; ce sont les onglets qui passent à la ligne quand le rail manque de place.

| Prop | Type | Rôle |
|---|---|---|
| `tabs` | `[{ id, label, icon, content, disabled }]` | Onglets ; `content` rend le panneau |
| `value`, `onChange` | | Onglet choisi (contrôlé) et changement |
| `defaultValue` | chaîne | Premier onglet choisi sans contrôle |
| `label` | chaîne traduite | Nom de la liste d'onglets |

### EmptyState

Message à la place d'une liste vide : texte gris centré, 64 px au-dessus et au-dessous.

| Prop | Type | Rôle |
|---|---|---|
| `message` | nœud | Texte, `common:status.noResults` par défaut |
| `title` | nœud | Titre en gras |
| `icon` | composant lucide | Icône décorative au-dessus |
| `actionLabel`, `onAction` | | Bouton primaire |
| `action` | nœud | Action libre, remplace le bouton |

### ErrorState

Message d'échec de chargement : texte rouge centré avec icône, `role="alert"`, bouton secondaire
Réessayer.

| Prop | Type | Rôle |
|---|---|---|
| `message` | nœud | Texte, `common:status.error` par défaut |
| `onRetry` | fonction | Affiche le bouton |
| `retryLabel` | chaîne | `common:actions.retry` par défaut |
| `retrying` | booléen | Spinner sur le bouton |

### Skeleton

Bloc gris pulsé (`bg-gray-100 animate-pulse` machinery), décoratif (`aria-hidden`) ; la pulsation
s'arrête sous mouvement réduit. Props `radius` (`control`, `tile`, `card`, `pill`) et `className`
pour la taille.

### ToastProvider, useToast

Notifications courtes, en bas à droite, dans une région `aria-live="polite"`. Carte blanche avec
l'icône du ton, bouton Masquer, fermeture automatique après `TOAST_DURATION` (5 s), en pause au
survol et au focus.

```jsx
// Une fois, autour de l'application
<ToastProvider>{children}</ToastProvider>

// Dans un écran
const toast = useToast()
toast.success(t('equipment.saved'))
toast.show({ message: t('equipment.saveFailed'), tone: 'error', duration: 0 })
```

| Fonction | Rôle |
|---|---|
| `show({ message, tone, duration })` | Affiche un toast (`success`, `error`, `warning`, `info`) ; `duration` 0 le garde ; renvoie son id |
| `success(message)`, `error(message)` | Raccourcis |
| `dismiss(id)` | Ferme un toast |

## Écarts avec machinery à valider par le propriétaire

| Sujet | Choix du kit |
|---|---|
| Bordure des champs | `border-input` (gray-300, 1,5:1) gardée, sous les 3:1 demandés |
| Lignes de tableau | Filets `border-b` gardés comme machinery ; la règle C-LAY-05 refuse les filets séparateurs |
| Espacements de 2 px | `py-0.5` des badges et `mt-0.5` des tuiles KPI gardés, hors de l'échelle de 4 px |
| Cibles | 40 px (boutons), 32 px (icônes), 28 px (pagination) sur ordinateur, 44 px au toucher |
| Entrées animées | Le fondu des cartes et des lignes de machinery n'est pas repris (rien ne part d'une opacité nulle au-dessus de la ligne de flottaison) |
| Titre de page | `text-2xl` du tableau de bord pour tous les écrans (les listes machinery utilisaient `text-xl`) |
| Libellé des tuiles KPI | `text-text` au lieu de gray-700 |
| Graisses | Quatre graisses (400, 500, 600, 700) comme machinery ; la règle en attend deux ou trois |
| Nouveaux tokens | `border-divider`, `neutral-bg`, `neutral-fg`, `danger`, `danger-pressed`, `backdrop` |
