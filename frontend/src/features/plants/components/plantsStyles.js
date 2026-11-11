/*
 * The styles shared by the screens of the Plants module, so that a card, a
 * table header, a button or a field is written once. Colours come from the
 * team tokens of index.css where one exists: the page is --color-background
 * (#f8f9ff), the main text --color-on-background (#0b1c30), the card shadow
 * --shadow-card. Contrast of the text colours used on them (WCAG AA is 4.5:1):
 *   #0b1c30 -> 17.17:1 on white, 16.34:1 on the page
 *   gray-800 #1e2939 -> 14.67:1 on white, 13.96:1 on the page
 *   gray-700 #364153 -> 10.30:1 on white, 9.81:1 on the page
 *   gray-600 #4a5565 -> 7.56:1 on white, 7.19:1 on the page
 *   gray-500 #6a7282 -> 4.84:1 on white, 4.60:1 on the page, 4.63:1 on gray-50
 */

/** The page behind the cards. */
export const PAGE = "min-h-dvh bg-background";

/** Frame of every card: 16 px radius, gray-200 border, white, the team's card shadow. */
export const CARD_FRAME = "rounded-2xl border border-gray-200 bg-white shadow-card";

/** A dashboard tile (Overview): 16 px inside, 16 px between tiles. */
export const CARD = `${CARD_FRAME} p-4`;

/** A section of a task screen (filters, forms): 24 px inside. */
export const SECTION_CARD = `${CARD_FRAME} p-6`;

/** The card around a table: no padding, and the table scrolls inside it. */
export const TABLE_CARD = `overflow-x-auto ${CARD_FRAME}`;

/** A column header: 12 px semibold in sentence case; gray-500 on gray-50 -> 4.63:1. */
export const TABLE_HEAD = "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold text-gray-500";

/** The one icon size inside the screens, in px. */
export const ICON_SIZE = 16;

/** Card titles: 18 px Montserrat 700, as set by the type scale. */
export const CARD_TITLE = "font-heading text-lg font-bold text-on-background";

/** Card captions and notes: 12 px; gray-600 #4a5565 on white -> 7.56:1. */
export const CARD_CAPTION = "text-xs text-gray-600";

/** The error box shared by the cards; gray-600 on the tinted box (#fcf6f5) -> 7.07:1. */
export const ERROR_BOX = "flex flex-wrap items-center justify-between gap-4 rounded-lg border border-error/30 bg-error/5 p-4";

/** A primary action, 40 px high: white on primary #0a8276 -> 4.70:1. */
export const PRIMARY_BUTTON =
    "inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60";

/** A secondary action, 40 px high: gray-700 on white -> 10.30:1. */
export const SECONDARY_BUTTON =
    "inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-gray-200 bg-white px-4 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60";

/** The label of a field or a filter: 12 px semibold in sentence case; gray-500 on white -> 4.84:1. */
export const FIELD_LABEL = "text-xs font-semibold text-gray-500";

/**
 * A text field, a date field or a select, 40 px high. Its text is 16 px, the
 * one written exception to the 24/18/14/12 scale: under 16 px, Safari on an
 * iPhone zooms the page when the field takes the focus. gray-800 on white ->
 * 14.67:1. At the keyboard the field shows a 2 px primary ring, #0a8276 ->
 * 4.70:1 on white and 4.47:1 on the page (3:1 is required for a focus mark).
 */
export const FIELD_INPUT =
    "h-10 rounded-lg border border-gray-200 bg-white px-3 text-base text-gray-800 focus:border-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:text-gray-400";

/**
 * A gray block standing for content that is loading. It is decoration: the
 * card that shows it also says "Loading…" to screen readers. index.css stops
 * the pulse for users who ask for reduced motion.
 */
export const SKELETON = "animate-pulse rounded-lg bg-gray-200";

/**
 * The columns of an alert row from lg up, shared with the header row of the
 * list: priority, where, issue, when, action. Below lg a row reads on two
 * lines, priority, where and issue, then when and its action, with the
 * message under them.
 */
export const ALERT_COLUMNS = "lg:grid-cols-[6.5rem_10rem_minmax(0,1fr)_11rem_3rem]";

/** A text link inside a card: primary-dark #065e55 on white -> 7.66:1. */
export const CARD_LINK =
    "text-sm font-semibold text-primary-dark underline underline-offset-2 hover:no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-dark";
