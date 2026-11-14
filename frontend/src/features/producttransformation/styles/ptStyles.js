/*
 * The styles shared by the screens of the Product Transformation module.
 *
 * This mirrors the Plants module's `plantsStyles.js` so that a card, a table
 * header, a button or a field is written once and reads the same on every PT
 * screen. Colours come from the team tokens of index.css where one exists:
 *   page          --color-background  (#f8f9ff)
 *   main text     --color-on-background (#0b1c30)
 *   primary       --color-primary     (#0a8276)
 *   card shadow   --shadow-card
 *
 * Contrast of the text colours used on white / on the page (WCAG AA is 4.5:1):
 *   on-background #0b1c30 -> 17.17:1 on white, 16.34:1 on the page
 *   gray-800      #1e2939 -> 14.67:1 on white, 13.96:1 on the page
 *   gray-700      #364153 -> 10.30:1 on white, 9.81:1 on the page
 *   gray-600      #4a5565 -> 7.56:1 on white, 7.19:1 on the page
 *   gray-500      #6a7282 -> 4.84:1 on white, 4.60:1 on the page, 4.63:1 on gray-50
 *
 * Grade and status chips pair a light tint with a dark text colour, never
 * colour alone: the label is always written beside the chip.
 */

/** The page behind the cards. */
export const PAGE = "min-h-dvh bg-background";

/** Frame of every card: 16 px radius, gray-200 border, white, the team's card shadow. */
export const CARD_FRAME = "rounded-2xl border border-gray-200 bg-white shadow-card";

/** A dashboard tile: 16 px inside, 16 px between tiles. */
export const CARD = `${CARD_FRAME} p-4`;

/** A section of a task screen (filters, forms): 24 px inside. */
export const SECTION_CARD = `${CARD_FRAME} p-6`;

/** The card around a table: no padding, and the table scrolls inside it. */
export const TABLE_CARD = `overflow-x-auto ${CARD_FRAME}`;

/** A column header: 12 px semibold in sentence case; gray-500 on gray-50 -> 4.63:1. */
export const TABLE_HEAD_BASE =
  "border-b border-gray-200 bg-gray-50 px-4 py-3 text-xs font-semibold text-gray-500";

/** The default left-aligned column header. */
export const TABLE_HEAD = `${TABLE_HEAD_BASE} text-left`;

/** The one icon size inside the screens, in px. */
export const ICON_SIZE = 16;

/** Card titles: 18 px Montserrat 700, as set by the type scale. */
export const CARD_TITLE = "font-heading text-lg font-bold text-on-background";

/** Page titles: 24 px Montserrat 700. */
export const PAGE_TITLE = "font-heading text-2xl font-bold text-on-background";

/** Page captions under the title: 14 px; gray-600 on the page -> 7.19:1. */
export const PAGE_SUBTITLE = "mt-1 text-sm text-gray-600";

/** Card captions and notes: 12 px; gray-600 on white -> 7.56:1. */
export const CARD_CAPTION = "text-xs text-gray-600";

/** The error box shared by the cards; gray-600 on the tinted box (#fcf6f5) -> 7.07:1. */
export const ERROR_BOX =
  "flex flex-wrap items-center justify-between gap-4 rounded-lg border border-error/30 bg-error/5 p-4";

/** A primary action, 40 px high: white on primary #0a8276 -> 4.70:1. */
export const PRIMARY_BUTTON =
  "inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

/** A secondary action, 40 px high: gray-700 on white -> 10.30:1. */
export const SECONDARY_BUTTON =
  "inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-gray-200 bg-white px-4 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

/** A destructive action, 40 px high. */
export const DANGER_BUTTON =
  "inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-red-200 bg-white px-4 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700";

/** The label of a field or a filter: 12 px semibold in sentence case; gray-500 on white -> 4.84:1. */
export const FIELD_LABEL = "text-xs font-semibold text-gray-500";

/**
 * A text field, a date field or a select, 40 px high. Its text is 16 px, the
 * one written exception to the 24/18/14/12 scale: under 16 px, Safari on an
 * iPhone zooms the page when the field takes the focus.
 */
export const FIELD_INPUT =
  "h-10 rounded-lg border border-gray-200 bg-white px-3 text-base text-gray-800 focus:border-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:text-gray-400";

/** A compact field used inside a filter bar (14 px is acceptable there). */
export const FILTER_INPUT =
  "h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 focus:border-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:text-gray-400";

/** A filter field with a leading icon: 36 px left padding leaves room for it. */
export const SEARCH_INPUT =
  "h-10 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 focus:border-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:text-gray-400";

/**
 * A gray block standing for content that is loading. It is decoration: the
 * card that shows it also says "Loading…" to screen readers.
 */
export const SKELETON = "animate-pulse rounded-lg bg-gray-200";

/** A text link inside a card: primary-dark #065e55 on white -> 7.66:1. */
export const CARD_LINK =
  "text-sm font-semibold text-primary-dark underline underline-offset-2 hover:no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-dark";

/** A pill badge: 12 px semibold. */
export const BADGE = "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold";

/** The icon chip of a KPI card. */
export const KPI_TONES = {
  primary: "bg-teal-50 text-primary",
  success: "bg-emerald-50 text-emerald-700",
  warning: "bg-amber-50 text-amber-700",
  error: "bg-red-50 text-red-700",
  info: "bg-sky-50 text-sky-700",
  purple: "bg-purple-50 text-purple-700",
  orange: "bg-orange-50 text-orange-700",
};

/*
 * Grade chips. The tints match the Plants module's badge hues so the two
 * modules read as one product. Each pairs a dark text colour with a light
 * background; the grade letter is always written, never colour alone.
 */
export const GRADE_TONES = {
  A: "bg-[#e5f7ef] text-[#257e55]",
  B: "bg-[#e4f2f0] text-primary-dark",
  C: "bg-[#f8efde] text-[#8e661c]",
  D: "bg-[#f7e9e7] text-[#9a3f32]",
};

/** Chart colours for the grades (same hues as the chips, readable on white). */
export const GRADE_CHART_COLORS = {
  A: "#2e9e6b",
  B: "#0a8276",
  C: "#d9a441",
  D: "#c1503f",
};

/** Chart colours for the batch lifecycle, one per status. */
export const STATUS_CHART_COLORS = {
  CREATED: "#6b7280",
  INTAKE: "#3a7ca5",
  WASHING: "#0fa396",
  DRYING: "#d9a441",
  PACKAGING: "#ef6c00",
  COMPLETED: "#2e9e6b",
  SHIPPED: "#8e24aa",
  REJECTED: "#c1503f",
};

export const getGradeTone = (grade) => GRADE_TONES[grade] ?? "bg-gray-100 text-gray-700";
