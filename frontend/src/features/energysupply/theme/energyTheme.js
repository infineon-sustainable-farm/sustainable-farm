/**
 * Exact colors and styles for the Energy Supply Systems module,
 * matching the validated mockup (team-corrected: primary = green
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
  primary: "#4caf50",
  primaryForeground: "#ffffff",
  secondary: "#0a8276",
  muted: "#eef2f1",
  mutedForeground: "#5c6b68",
  accent: "#e3efed",
  destructive: "#c62828",
  warning: "#ef6c00",
  success: "#4caf50",
  border: "#dbe3e1",
  chart: ["#4caf50", "#ef6c00", "#c62828", "#4a9d93", "#7fb8b1"],
};

export const fontHeading = "Inter, ui-sans-serif, system-ui, sans-serif";

