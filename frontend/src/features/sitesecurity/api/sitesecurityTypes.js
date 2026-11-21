/*
 * Access levels, from the least to the most sensitive: a level also covers the zones below it.
 * Colours are design tokens of src/index.css (no hex): the badge tone of the shared kit, and the
 * CSS variables the diagram and the meters read.
 */
export const LEVEL_ORDER = ["open", "controlled", "restricted", "critical"];

const UNKNOWN_LEVEL = {
  id: "unknown",
  tone: "neutral",
  meter: 0,
  solid: "var(--color-neutral-fg)",
  fill: "var(--color-surface-muted)",
};

export const LEVEL_META = {
  open: { id: "open", tone: "neutral", meter: 1, solid: "var(--color-neutral-fg)", fill: "var(--color-surface-muted)" },
  controlled: { id: "controlled", tone: "info", meter: 2, solid: "var(--color-info-fg)", fill: "var(--color-info-tint)" },
  restricted: { id: "restricted", tone: "warning", meter: 3, solid: "var(--color-warning-fg)", fill: "var(--color-warning-tint)" },
  critical: { id: "critical", tone: "error", meter: 4, solid: "var(--color-error-fg)", fill: "var(--color-error-tint)" },
};

/* A value the frontend does not know must not break a whole screen. */
export function levelMeta(level) {
  return LEVEL_META[level] ?? UNKNOWN_LEVEL;
}
