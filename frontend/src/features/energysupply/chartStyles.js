// Recharts settings of the energy charts, on the tokens of src/index.css, as the machinery fuel
// chart. The axis ticks are SVG attributes: their font size is the text-xs value as a number.
const TEXT_XS = 12;

export const AXIS_TICK = { fill: "var(--color-chart-3)", fontSize: TEXT_XS };
export const GRID_STROKE = "var(--color-chart-grid)";
export const TOOLTIP_STYLE = {
  borderRadius: "var(--radius-tile)",
  border: "thin solid var(--color-border-divider)",
  fontSize: "var(--text-xs)",
};
export const BAR_RADIUS = [6, 6, 0, 0];

/** Series colours of the consumption breakdown, one per load category. */
export const SERIES_COLORS = [
  "var(--color-chart-1)",
  "var(--color-chart-2)",
  "var(--color-chart-3)",
  "var(--color-chart-4)",
  "var(--color-chart-5)",
  "var(--color-chart-6)",
];

/** Legend swatches of the same series, as Tailwind classes of the chart tokens. */
export const SERIES_SWATCHES = ["bg-chart-1", "bg-chart-2", "bg-chart-3", "bg-chart-4", "bg-chart-5", "bg-chart-6"];
