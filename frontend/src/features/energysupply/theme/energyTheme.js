/**
 * Exact colors and styles for the Energy Supply Systems module,
 * matching the validated mockup (primary = #0a8276, secondary =
 * #4caf50, white-background logo, harmonized font sizes).
 *
 * These are kept local to this feature on purpose: editing the
 * shared global theme (frontend/src/index.css) requires
 * coordination with other module owners per CONTRIBUTING.md.
 * Once the team agrees on a unified design system, these can be
 * migrated into the shared Tailwind theme.
 */
export const energyColors = {
  background: "#f5f7fa",
  foreground: "#1a2422",
  card: "#ffffff",
  primary: "#0a8276",
  primaryForeground: "#ffffff",
  secondary: "#4caf50",
  muted: "#eef2f1",
  mutedForeground: "#5c6b68",
  accent: "#e3efed",
  destructive: "#c62828",
  warning: "#ef6c00",
  success: "#4caf50",
  border: "#dbe3e1",
  chart: ["#0a8276", "#4caf50", "#ef6c00", "#c62828", "#7fb8b1"],
};

export const fontHeading = "Inter, ui-sans-serif, system-ui, sans-serif";
