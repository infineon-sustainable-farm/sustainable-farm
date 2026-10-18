export const SCREEN_LABEL = {
    dashboard: "Dashboard",
    access: "Access Control",
    zones: "Perimeter & Zones",
    log: "Access Log",
};

export const LEVEL_ORDER = ["open", "controlled", "restricted", "critical"];
export const LEVEL_META = {
    open: {
        label: "Open",
        solid: "#64748B",
        chip: "bg-slate-50 text-slate-600 border-slate-200",
        dot: "bg-slate-400",
        avatar: "bg-slate-100 text-slate-600",
        meter: 1,
        fill: "#F7F9FB",
        stroke: "#94A3B8",
    },
    controlled: {
        label: "Controlled",
        solid: "#2F6BFF",
        chip: "bg-blue-50 text-blue-700 border-blue-200",
        dot: "bg-blue-500",
        avatar: "bg-blue-50 text-blue-700",
        meter: 2,
        fill: "#EEF3FE",
        stroke: "#2F6BFF",
    },
    restricted: {
        label: "Restricted",
        solid: "#D97706",
        chip: "bg-amber-50 text-amber-700 border-amber-200",
        dot: "bg-amber-500",
        avatar: "bg-amber-50 text-amber-700",
        meter: 3,
        fill: "#FDF6E7",
        stroke: "#D97706",
    },
    critical: {
        label: "Critical",
        solid: "#E11D48",
        chip: "bg-rose-50 text-rose-700 border-rose-200",
        dot: "bg-rose-500",
        avatar: "bg-rose-50 text-rose-700",
        meter: 4,
        fill: "#FDECEF",
        stroke: "#E11D48",
    },
};
