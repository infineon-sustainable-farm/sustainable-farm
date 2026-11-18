import { CircleAlert, TriangleAlert } from "lucide-react";

// Banners at the top of the machinery forms: server error (red) and missing equipment (amber).
const TONES = {
  error: { className: "bg-error-tint text-error-fg", icon: CircleAlert, role: "alert" },
  warning: { className: "bg-warning-tint text-warning-fg", icon: TriangleAlert, role: undefined },
};

function FormAlert({ tone = "error", children }) {
  const { className, icon: Icon, role } = TONES[tone] ?? TONES.error;
  return (
    <p role={role} className={`mb-4 flex items-center gap-2 rounded-control px-3 py-2 text-sm ${className}`}>
      <Icon size={16} aria-hidden="true" className="shrink-0" />
      <span>{children}</span>
    </p>
  );
}
export default FormAlert;
