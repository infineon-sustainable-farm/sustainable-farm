import {
  AlertTriangle,
  ArrowUpRight,
  ChevronsRight,
  DoorOpen,
  HardHat,
  Layers3,
  ShieldAlert,
  ShieldCheck,
  Truck,
  UserRound,
  UserRoundCheck,
} from "lucide-react";
import { cn } from "../utils/cn";
import PerimeterMap from "../components/PerimeterMap";
import { Card, Eyebrow, ScreenHeader, StatusChip } from "../components/ui";
import { LEVEL_META, type LevelKey, type LogFilter } from "../api/sitesecurityTypes";
import { useSecurityOverview } from "../hooks/useSecurityOverview";
import { useAccessLogs } from "../hooks/useAccessLogs";

const MODEL_STEPS = [
  "Define the perimeter",
  "Divide into zones",
  "Assign access levels",
  "Control who enters",
  "Monitor activity",
  "Record denials",
];

const STATS = [
  { label: "Active Access Points", value: "4", icon: DoorOpen, tint: "bg-navy-50 text-navy-900", sub: "All responding · sweep 10:30", dot: "bg-emerald-500", pulse: false },
  { label: "Controlled Zones", value: "3", icon: Layers3, tint: "bg-blue-50 text-blue-700", sub: "Of 4 zones defined", dot: "bg-blue-500", pulse: false },
  { label: "Active Visitors", value: "2", icon: UserRoundCheck, tint: "bg-emerald-50 text-emerald-700", sub: "On site now", dot: "bg-emerald-500", pulse: false },
  { label: "Alerts", value: "1", icon: AlertTriangle, tint: "bg-rose-50 text-rose-600", sub: "Unresolved denial — 10:27", dot: "bg-rose-500", pulse: true, alert: true },
];

const FEED_IDS = ["l2", "l3", "l5", "l6"];

const FEED_ICON: Record<string, typeof HardHat> = {
  Staff: HardHat,
  Visitor: UserRound,
  Service: Truck,
  Unknown: ShieldAlert,
};

const FEED_TINT: Record<string, string> = {
  Staff: "bg-blue-50 text-blue-600",
  Visitor: "bg-slate-100 text-slate-500",
  Service: "bg-amber-50 text-amber-600",
  Unknown: "bg-rose-50 text-rose-600",
};

export default function Dashboard({
  onOpenZones,
  onOpenLog,
}: {
  onOpenZones: (zone?: LevelKey) => void;
  onOpenLog: (filter?: LogFilter) => void;
}) {
  const { gates } = useSecurityOverview();
  const { allLogs } = useAccessLogs();

  const feed = FEED_IDS.map((id) => allLogs.find((l) => l.id === id)).filter(Boolean).sort((a, b) =>
    a!.time.localeCompare(b!.time)
  );

  return (
    <div className="space-y-5">
      <ScreenHeader
        kicker="01 · Overview"
        title="Security Overview"
        subtitle="Farm perimeter & access control"
        actions={
          <span className="hidden items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-[11px] font-semibold uppercase tracking-wider text-emerald-700 lg:flex">
            <span className="size-1.5 rounded-full bg-emerald-500 status-pulse" />
            Perimeter armed
          </span>
        }
      />

      {/* operating model */}
      <Card className="flex flex-wrap items-center gap-x-1 gap-y-2 px-5 py-3.5">
        {MODEL_STEPS.map((step, i) => (
          <div key={step} className="flex items-center">
            {i > 0 && <ChevronsRight className="mx-2 size-3.5 text-slate-300" strokeWidth={2.4} />}
            <span className="flex items-center gap-2">
              <span className="font-mono text-[11px] font-semibold text-primary-600">
                {String(i + 1).padStart(2, "0")}
              </span>
              <span className="text-[13px] font-medium text-navy-900 font-sans">{step}</span>
            </span>
          </div>
        ))}
      </Card>

      {/* summary cards */}
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {STATS.map((s) => {
          const Icon = s.icon;
          return (
            <button
              key={s.label}
              onClick={() => (s as { alert?: boolean }).alert ? onOpenLog("denied") : undefined}
              className={cn(
                "rounded-2xl border bg-white p-5 text-left shadow-[0_1px_2px_rgba(16,27,46,0.04),0_10px_30px_-14px_rgba(16,27,46,0.12)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_2px_4px_rgba(16,27,46,0.05),0_16px_36px_-14px_rgba(16,27,46,0.18)]",
                (s as { alert?: boolean }).alert ? "border-rose-200/80" : "border-slate-200/80"
              )}
            >
              <div className="flex items-start justify-between">
                <span className={cn("flex size-10 items-center justify-center rounded-xl", s.tint)}>
                  <Icon className="size-[18px]" strokeWidth={2.2} />
                </span>
                {(s as { alert?: boolean }).alert && <ArrowUpRight className="size-4 text-rose-500" />}
              </div>
              <p className="mt-4 font-display text-[30px] font-bold leading-none tracking-tight text-navy-900">
                {s.value}
              </p>
              <p className="mt-1.5 text-[13px] font-medium text-slate-500 font-sans">{s.label}</p>
              <p className="mt-2.5 flex items-center gap-1.5 text-[12px] text-slate-400 font-sans">
                <span className={cn("size-1.5 rounded-full", s.dot, s.pulse && "status-pulse")} />
                {s.sub}
              </p>
            </button>
          );
        })}
      </div>

      {/* perimeter status */}
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <div>
            <h2 className="font-display text-[17px] font-semibold text-navy-900">Perimeter Status</h2>
            <p className="mt-0.5 text-[12px] text-slate-400 font-sans">
              Conceptual layout — not a geographic map
            </p>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden items-center gap-3 md:flex">
              {(["open", "controlled", "restricted", "critical"] as LevelKey[]).map((l) => (
                <span key={l} className="flex items-center gap-1.5 font-sans text-[12px] font-medium text-slate-500">
                  <span className="size-2 rounded-sm" style={{ backgroundColor: LEVEL_META[l].solid }} />
                  {LEVEL_META[l].label}
                </span>
              ))}
            </div>
            <button
              onClick={() => onOpenZones()}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3.5 py-1.5 text-[13px] font-semibold text-navy-900 transition-all hover:border-primary-200 hover:bg-primary-50 hover:text-primary-600 font-sans"
            >
              Perimeter & Zones
              <ArrowUpRight className="size-3.5" />
            </button>
          </div>
        </div>

        <div className="grid lg:grid-cols-[minmax(0,1fr)_300px]">
          <div className="bg-dotgrid p-4">
            <PerimeterMap
              compact
              selected={null}
              onSelect={(zone) => onOpenZones(zone)}
            />
            <p className="-mt-1 pb-1 text-center font-mono text-[9.5px] uppercase tracking-[0.16em] text-slate-400">
              Select a zone to open detail view
            </p>
          </div>

          <div className="flex flex-col border-t border-slate-100 lg:border-l lg:border-t-0">
            <div className="px-5 pt-5">
              <Eyebrow>Access points</Eyebrow>
            </div>
            <div className="flex-1 divide-y divide-slate-100 px-5">
              {gates.map((g) => (
                <div key={g.id} className="flex items-center gap-3 py-3.5">
                  <span
                    className="flex size-8 items-center justify-center rounded-lg border"
                    style={{
                      backgroundColor: LEVEL_META[g.zone].fill,
                      borderColor: `${LEVEL_META[g.zone].solid}30`,
                      color: LEVEL_META[g.zone].solid,
                    }}
                  >
                    <DoorOpen className="size-3.5" strokeWidth={2.2} />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12.5px] font-semibold text-navy-900">{g.name}</p>
                    <p className="truncate text-[10.5px] text-slate-400">{g.detail}</p>
                  </div>
                  <StatusChip status={g.status} size="sm" />
                </div>
              ))}
            </div>
            <div className="px-5 pb-5 pt-1">
              <div className="rounded-xl bg-slate-50 px-4 py-3.5">
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-1.5 text-[11px] font-semibold text-navy-900">
                    <ShieldCheck className="size-3.5 text-emerald-600" />
                    Perimeter integrity
                  </span>
                  <span className="font-mono text-[11px] font-semibold text-emerald-600">100%</span>
                </div>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200/70">
                  <div className="h-full w-full rounded-full bg-gradient-to-r from-emerald-500 to-emerald-400" />
                </div>
                <p className="mt-2 font-mono text-[9.5px] uppercase tracking-[0.14em] text-slate-400">
                  96/96 sensors · checked 10:30
                </p>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* recent activity */}
      <Card>
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Recent Access Activity</h2>
          <button
            onClick={() => onOpenLog()}
            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-[12px] font-semibold text-navy-900 transition-all hover:border-accent-200 hover:bg-accent-50 hover:text-accent-600"
          >
            View full log
            <ArrowUpRight className="size-3.5" />
          </button>
        </div>
        <div className="divide-y divide-slate-100 px-6">
          {feed.map((e) => {
            const Icon = FEED_ICON[e.type] ?? UserRound;
            return (
              <button
                key={e.id}
                onClick={() => onOpenLog()}
                className="flex w-full items-center gap-4 py-3.5 text-left transition-colors hover:bg-slate-50/50"
              >
                <span className={cn("flex size-9 shrink-0 items-center justify-center rounded-xl", FEED_TINT[e.type])}>
                  <Icon className="size-4" strokeWidth={2.2} />
                </span>
                <p className="flex-1 text-[13px] text-slate-600">
                  <span className="font-semibold text-navy-900">{e.user}</span>
                  {e.type === "Unknown"
                    ? " attempted entry at "
                    : e.action === "Entry"
                      ? " entered "
                      : " exited "}
                  {e.zoneName}
                </p>
                <StatusChip status={e.status} size="sm" />
                <span className="w-12 text-right font-mono text-[11.5px] font-medium text-slate-400">
                  {e.time}
                </span>
              </button>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
