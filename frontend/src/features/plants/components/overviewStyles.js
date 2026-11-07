/*
 * The one card of the Overview: the same radius, border, background and
 * padding for the figures, the alerts and the charts. Spacing stays on the
 * 8 px grid: 16 px inside a card, 16 px between cards.
 */
export const CARD = "rounded-xl border border-gray-200 bg-white p-4";

/** The one icon size of the Overview and its sidebar, in px. */
export const ICON_SIZE = 16;

/** Card titles: 18 px Montserrat 700, as set by the type scale. */
export const CARD_TITLE = "font-heading text-lg font-bold text-gray-900";

/** Card captions and notes: 12 px; gray-600 #4a5565 on white -> 7.56:1. */
export const CARD_CAPTION = "text-xs text-gray-600";

/** The error box shared by the cards; gray-600 on the tinted box (#f6f1f1) -> 6.76:1. */
export const ERROR_BOX = "flex flex-wrap items-center justify-between gap-4 rounded-lg border border-error/30 bg-error/5 p-4";

/** A primary action inside a card, such as Retry. */
export const PRIMARY_BUTTON =
    "rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60";

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
