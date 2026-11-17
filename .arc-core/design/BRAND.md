# Marque Sustainable Farm

Back-office of a farm, one web application with one module per domain (machinery, plants, product
transformation, sales and marketing, visitor management, water supply, energy supply). The reference look
is the one of the machinery module (`frontend/src/features/machinery`): every module gets the same
rendering. The tokens of this folder are the values machinery renders today.

## Questionnaire de goût

1. Reference: the machinery module of this application (sidebar, cards, KPI tiles, tables, forms).
2. To avoid: the dark theme of the former site security screens, Material-style colour sets, mixed icon sets.
3. Theme: light only. The dark theme of `semantic/dark.json` is a placeholder kept for parity, not designed.
4. Mood: sober, technical, calm.
5. Humans: no portraits; one photo banner on the machinery dashboard.
6. Motion: calm. Colour transitions 150 ms, drawer 300 ms, card entrance 550 ms; all motion off under
   `prefers-reduced-motion`.
7. Accent: teal `#0A8276` (sidebar, primary buttons, first chart series). Font: Inter for body and headings
   (SIL Open Font License 1.1, Google Fonts). No display font.
8. Icons: Lucide (`lucide-react`), stroke 2, sizes 16, 18 and 20 px.
9. Tone: product interface in English, German and French; short labels, no marketing words.
10. Main action: record and follow farm operations (one primary button per screen).
11. Never: personal data in examples, private names, hard-coded user names.

## Valeurs de machinery

| Element | Rendering | Token |
|---|---|---|
| Page background | `#F8F9FF` | `color.background.body` |
| Body text | `#0B1C30` | `color.text.primary` |
| Secondary text | gray-500 `#6A7282` | `color.text.secondary` |
| Sidebar | `bg-primary`, 240 px, links 36 px, `rounded-lg` | `color.accent.default`, `layout.sidebar`, `size.control-sm`, `radius.sm` |
| Card | white, `rounded-2xl`, gray-100 border, `shadow-sm`, 20 px padding | `color.background.surface`, `radius.lg`, `color.border.default`, `space.5` |
| KPI icon tile | 40 px, `rounded-xl`, teal/amber/red/sky-50 | `size.control-md`, `radius.md` |
| Button | primary, white text, `rounded-lg`, 16 x 8 px padding | `color.accent.*`, `radius.sm` |
| Input | gray-300 border, `rounded-lg`, 12 x 8 px padding | `radius.sm` |
| Badge | `rounded-full`, 12 px semibold, green/amber/red-100 with -700 text | `radius.pill`, `color.status.*`, `color.text.success/warning/error` |
| Charts | `#0A8276`, `#D9A441`, grid `#EEF2F1`, ticks `#94A3B8` | `color.chart.*` |

## Choix de marque (écarts assumés avec le core)

- Shadows: yes, two (`shadow-sm` for cards, `shadow-md` on hover and popovers). Gradients and glows: no.
- Radii: 8, 12, 16 px and pill (24 px kept for media).
- Weights: four (400, 500, 600, 700) because machinery uses all four; the check warns (C-TOK-04, 2 to 3
  expected). To reduce when the kit is built (T9) if the owner accepts a change of rendering.
- Primary text on the page background: `#0A8276` on `#F8F9FF` is 4.47:1 (below 4.5:1), on white it is
  4.70:1. `color.text.accent` therefore points to the darker teal `#065E55`; fills stay `#0A8276`.
- Strong border: `color.border.strong` is gray-500 (4.84:1 on white, 3:1 required). Machinery inputs use gray-300
  (`#D1D5DC`, 1.5:1 on white), kept as the input border to keep the rendering; to review in T9.
- Hint text: machinery uses gray-400 (`#99A1AF`, 2.6:1 on white) for hints and dates; not a token, to replace by `color.text.secondary` in T12.
- Language switcher: in the sidebar and on the launcher page (T11).

## Sections validées (gelées)

None yet. The rendering of machinery on the shared kit is validated by the owner in T50.
