/*
 * Classes shared by the sections of the Scenario Analysis screen: their plain tables (a row header
 * and a total row, which the kit Table does not draw) on the machinery table look, the figure
 * labels and notes, and the tone of a gain or a loss.
 */

/** Header cell of a scenario table. */
export const TH_CLASS = "py-2 pr-4 text-left align-bottom text-sm font-normal text-text-muted";
/** Header row and body row of a scenario table. */
export const HEAD_ROW_CLASS = "border-b border-border-divider";
export const ROW_CLASS = "border-b border-border-subtle";
/** Row header and cell of a scenario table. */
export const ROW_HEADER_CLASS = "py-2 pr-4 text-left font-medium text-text";
export const TD_CLASS = "py-2 pr-4 text-text";
/** Small uppercase label above a figure. */
export const FIGURE_LABEL_CLASS = "text-xs font-semibold tracking-wide text-text-muted uppercase";
/** Note under a figure. */
export const NOTE_CLASS = "text-xs text-text-muted";

/** A gain in green, a loss in red; zero and missing values keep `neutral`. The sign says it too. */
export function toneOf(value, neutral = "text-text") {
    if (value === null || value === undefined || value === 0) return neutral;
    return value > 0 ? "text-success-fg" : "text-error-fg";
}
