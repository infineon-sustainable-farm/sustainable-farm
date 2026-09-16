import type {
  GateDef,
  LevelKey,
  LevelMeta,
  LogEntry,
  User,
  ZoneDef,
} from "./sitesecurityTypes";
export { LEVEL_META, LEVEL_ORDER } from "./sitesecurityTypes";

export const ZONES: ZoneDef[] = [
  {
    id: "open",
    name: "Open Zone",
    tagline: "Visitor-facing areas",
    authorizedUsers: 24,
    entryPoints: 2,
    status: "Open Access",
    description:
      "Visitor-facing areas around the main entrance, reception and parking. The only part of the farm reachable without a farm-issued credential.",
    requirements: [
      "Visitor check-in at gate",
      "Visible ID badge while on site",
      "All access recorded",
    ],
  },
  {
    id: "controlled",
    name: "Controlled Zone",
    tagline: "Daily operations",
    authorizedUsers: 18,
    entryPoints: 2,
    status: "Monitored",
    description:
      "Barns, equipment yards and day-to-day operational areas. Staff credentials are required at every gate, and all movement is logged.",
    requirements: [
      "Staff credential required at gates",
      "Controlled entry points only",
      "All access recorded",
    ],
  },
  {
    id: "restricted",
    name: "Restricted Zone",
    tagline: "Machinery & utilities",
    authorizedUsers: 12,
    entryPoints: 1,
    status: "Protected",
    description:
      "Machinery storage, fuel and utility infrastructure. Access is limited to authorised personnel and routed through a single guarded entry point.",
    requirements: [
      "Authorized personnel only",
      "Controlled entry",
      "All access recorded",
    ],
  },
  {
    id: "critical",
    name: "Critical Zone",
    tagline: "High-value containment",
    authorizedUsers: 4,
    entryPoints: 1,
    status: "Locked Down",
    description:
      "Chemical store and high-value asset containment. Entry requires dual verification and raises a silent notification to the security lead.",
    requirements: [
      "Dual verification (card + PIN)",
      "Supervisor notified on every entry",
      "All access recorded + alert on denial",
    ],
  },
];

export const GATES: GateDef[] = [
  {
    id: "main",
    name: "Main Entrance",
    short: "Main",
    zone: "open",
    status: "Active",
    detail: "Scheduled 06:00 — 18:00",
  },
  {
    id: "visitor",
    name: "Visitor Gate",
    short: "Visitor",
    zone: "open",
    status: "Active",
    detail: "QR pass check-in",
  },
  {
    id: "staff",
    name: "Staff / Operational Gate",
    short: "Staff",
    zone: "controlled",
    status: "Secure",
    detail: "Staff credential required",
  },
  {
    id: "service",
    name: "Service / Emergency Gate",
    short: "Service",
    zone: "restricted",
    status: "Locked",
    detail: "Supervisor override only",
  },
];

export const USERS: User[] = [
  { id: "u1", name: "John Miller", role: "Grounds Supervisor", type: "Staff", level: "controlled", validUntil: "31 Dec 2026", status: "Active", initials: "JM", lastActive: "Today · 08:42" },
  { id: "u2", name: "Sarah Adams", role: "Visitor — estate tour", type: "Visitor", level: "open", validUntil: "12 May 2026", status: "Active", initials: "SA", lastActive: "Today · 09:15" },
  { id: "u3", name: "Maintenance Team", role: "Field operations", type: "Service", level: "restricted", validUntil: "30 Jun 2026", status: "Authorized", initials: "MT", lastActive: "Today · 10:03" },
  { id: "u4", name: "Marcus Webb", role: "Security lead", type: "Security", level: "critical", validUntil: "31 Dec 2026", status: "Authorized", initials: "MW", lastActive: "Today · 11:02" },
  { id: "u5", name: "Emma Rodriguez", role: "Visitor — supplier meeting", type: "Visitor", level: "open", validUntil: "12 May 2026", status: "Active", initials: "ER", lastActive: "Today · 09:58" },
  { id: "u6", name: "David Chen", role: "Barn operations", type: "Staff", level: "controlled", validUntil: "31 Dec 2026", status: "Active", initials: "DC", lastActive: "Today · 10:41" },
  { id: "u7", name: "Priya Nair", role: "Veterinary contractor", type: "Service", level: "restricted", validUntil: "02 May 2026", status: "Expired", initials: "PN", lastActive: "4 days ago" },
];

export const LOGS: LogEntry[] = [
  { id: "l9", ref: "EVT-1055", time: "11:24", date: "Tue 12 May 2026", user: "AgriSupply Delivery", initials: "AD", type: "Service", zone: "restricted", zoneName: "Restricted Zone", action: "Entry", status: "Pending", gate: "Service / Emergency Gate", method: "Service token — awaiting supervisor", note: "Unscheduled delivery. Holding at gate pending supervisor approval." },
  { id: "l8", ref: "EVT-1051", time: "11:02", date: "Tue 12 May 2026", user: "Marcus Webb", initials: "MW", type: "Security", zone: "critical", zoneName: "Critical Zone", action: "Exit", status: "Approved", gate: "Service / Emergency Gate", method: "Card + PIN", note: "Post-incident sweep completed after EVT-1048 denial." },
  { id: "l7", ref: "EVT-1049", time: "10:41", date: "Tue 12 May 2026", user: "David Chen", initials: "DC", type: "Staff", zone: "controlled", zoneName: "Controlled Zone", action: "Exit", status: "Approved", gate: "Staff / Operational Gate", method: "Staff card", note: "Break — off-site supply run." },
  { id: "l6", ref: "EVT-1048", time: "10:27", date: "Tue 12 May 2026", user: "Unknown User", initials: "?", type: "Unknown", zone: "critical", zoneName: "Critical Zone", action: "Entry", status: "Denied", gate: "Service / Emergency Gate", method: "No valid credential presented", note: "Unrecognised card presented twice. Denied at controller and recorded for review. Security lead notified." },
  { id: "l5", ref: "EVT-1046", time: "10:03", date: "Tue 12 May 2026", user: "Maintenance Team", initials: "MT", type: "Service", zone: "restricted", zoneName: "Restricted Zone", action: "Entry", status: "Approved", gate: "Service / Emergency Gate", method: "Service token", note: "Scheduled fence-line inspection — section B." },
  { id: "l4", ref: "EVT-1043", time: "09:58", date: "Tue 12 May 2026", user: "Emma Rodriguez", initials: "ER", type: "Visitor", zone: "open", zoneName: "Open Zone", action: "Entry", status: "Approved", gate: "Visitor Gate", method: "QR visitor pass", note: "Supplier meeting — escorted by Sarah in reception." },
  { id: "l3", ref: "EVT-1041", time: "09:15", date: "Tue 12 May 2026", user: "Sarah Adams", initials: "SA", type: "Visitor", zone: "open", zoneName: "Open Zone", action: "Entry", status: "Approved", gate: "Visitor Gate", method: "QR visitor pass", note: "Estate tour — invited by farm manager." },
  { id: "l2", ref: "EVT-1038", time: "08:42", date: "Tue 12 May 2026", user: "John Miller", initials: "JM", type: "Staff", zone: "controlled", zoneName: "Controlled Zone", action: "Entry", status: "Approved", gate: "Staff / Operational Gate", method: "Staff card", note: "Shift start — grounds team." },
  { id: "l1", ref: "EVT-1031", time: "08:12", date: "Tue 12 May 2026", user: "Marcus Webb", initials: "MW", type: "Security", zone: "critical", zoneName: "Critical Zone", action: "Entry", status: "Approved", gate: "Service / Emergency Gate", method: "Card + PIN", note: "Opening inspection round." },
];
