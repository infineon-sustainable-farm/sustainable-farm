import { CircleAlert, TriangleAlert } from "lucide-react";

// Banners of the visitor management forms: server error (red) and warning (amber).
const TONES = {
    error: { className: "bg-error-tint text-error-fg", icon: CircleAlert, role: "alert" },
    warning: { className: "bg-warning-tint text-warning-fg", icon: TriangleAlert, role: undefined },
};

export default function FormAlert({ tone = "error", className = "", children }) {
    const { className: toneClass, icon: Icon, role } = TONES[tone] ?? TONES.error;
    return (
        <p role={role} className={`flex items-center gap-2 rounded-control px-3 py-2 text-sm ${toneClass} ${className}`}>
            <Icon size={16} aria-hidden="true" className="shrink-0" />
            <span>{children}</span>
        </p>
    );
}
