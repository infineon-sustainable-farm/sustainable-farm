export const LEVEL_ORDER = ["open", "controlled", "restricted", "critical"] as const;
export type LevelKey = (typeof LEVEL_ORDER)[number];

export interface LevelMeta {
  label: string;
  solid: string;
  chip: string;
  dot: string;
  avatar: string;
  meter: number;
  fill: string;
  stroke: string;
}

export const LEVEL_META: Record<LevelKey, LevelMeta> = {
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

export interface ZoneDef {
  id: LevelKey;
  name: string;
  tagline: string;
  authorizedUsers: number;
  entryPoints: number;
  status: "Open Access" | "Monitored" | "Protected" | "Locked Down";
  description: string;
  requirements: string[];
}

export interface GateDef {
  id: string;
  name: string;
  short: string;
  zone: LevelKey;
  status: "Active" | "Secure" | "Locked";
  detail: string;
}

export type UserType = "Staff" | "Visitor" | "Service" | "Security";

export interface User {
  id: string;
  name: string;
  role: string;
  type: UserType;
  level: LevelKey;
  validUntil: string;
  status: "Active" | "Authorized" | "Expired";
  initials: string;
  lastActive: string;
}

export interface LogEntry {
  id: string;
  ref: string;
  time: string;
  date: string;
  user: string;
  initials: string;
  type: UserType | "Unknown";
  zone: LevelKey;
  zoneName: string;
  action: "Entry" | "Exit";
  status: "Approved" | "Denied" | "Pending";
  gate: string;
  method: string;
  note: string;
}

export type LogFilter = "all" | "approved" | "denied" | "visitors" | "staff" | "service";

export interface OverviewStats {
  activePoints: number;
  controlledZones: number;
  visitors: number;
  alerts: number;
}
