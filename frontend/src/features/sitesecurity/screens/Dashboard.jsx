import { AlertTriangle, ArrowUpRight, ChevronsRight, DoorOpen, HardHat, Layers3, ShieldAlert, ShieldCheck, TriangleAlert, Truck, UserRound, UserRoundCheck, } from "lucide-react";
import { cn } from "../utils/cn";
import PerimeterMap from "../components/PerimeterMap";
import { Card, Eyebrow, ScreenHeader, StatusChip } from "../components/ui";
import { LEVEL_META, levelMeta } from "../api/sitesecurityTypes";
import { useSecurityOverview } from "../hooks/useSecurityOverview";
import { useAccessLogs } from "../hooks/useAccessLogs";
import { recentEvents } from "../utils/accessLogs";
import { buildStatCards } from "../utils/dashboardStats";
const MODEL_STEPS = [
    "Define the perimeter",
    "Divide into zones",
    "Assign access levels",
    "Control who enters",
    "Monitor activity",
    "Record denials",
];
const STAT_STYLE = {
    points: { icon: DoorOpen, tint: "bg-navy-50 text-navy-900", dot: "bg-emerald-500" },
    zones: { icon: Layers3, tint: "bg-blue-50 text-blue-700", dot: "bg-blue-500" },
    visitors: { icon: UserRoundCheck, tint: "bg-emerald-50 text-emerald-700", dot: "bg-emerald-500" },
    denied: { icon: AlertTriangle, tint: "bg-rose-50 text-rose-600", dot: "bg-rose-500" },
};
const FEED_ICON = {
    Staff: HardHat,
    Visitor: UserRound,
    Service: Truck,
    Security: ShieldCheck,
    Unknown: ShieldAlert,
};
const FEED_TINT = {
    Staff: "bg-blue-50 text-blue-600",
    Visitor: "bg-slate-100 text-slate-600",
    Service: "bg-amber-50 text-amber-600",
    Security: "bg-emerald-50 text-emerald-600",
    Unknown: "bg-rose-50 text-rose-600",
};
export default function Dashboard({ onOpenZones, onOpenLog, }) {
    const { gates, stats, isLoading: isLoadingOverview, error: overviewError } = useSecurityOverview();
    const { allLogs, isLoading: isLoadingLogs, error: logsError } = useAccessLogs();
    const error = overviewError ?? logsError;
    const feed = recentEvents(allLogs);
    const cards = buildStatCards(stats, gates.length);
    return (<div className="space-y-5">
      <ScreenHeader kicker="01 · Overview" title="Security Overview" subtitle="Farm perimeter & access control" actions={<span className="hidden items-center gap-2 rounded-full border border-dashed border-amber-300 bg-amber-50 px-3.5 py-2 font-mono text-[10px] font-semibold uppercase tracking-[0.14em] text-amber-700 lg:inline-flex">
            <TriangleAlert className="size-3.5"/>
            No live sensors connected
          </span>}/>

      {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
        {error}
      </div>)}

      {/* operating model */}
      <Card className="flex flex-wrap items-center gap-x-1 gap-y-2 px-5 py-3.5">
        {MODEL_STEPS.map((step, i) => (<div key={step} className="flex items-center">
            {i > 0 && <ChevronsRight className="mx-2 size-3.5 text-slate-300" strokeWidth={2.4}/>}
            <span className="flex items-center gap-2">
              <span className="font-mono text-[11px] font-semibold text-primary-700">
                {String(i + 1).padStart(2, "0")}
              </span>
              <span className="text-[13px] font-medium text-navy-900 font-sans">{step}</span>
            </span>
          </div>))}
      </Card>

      {/* summary cards */}
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {cards.map((s) => {
            const style = STAT_STYLE[s.id];
            const Icon = style.icon;
            const StatCard = s.alert ? "button" : "div";
            const action = s.alert ? { type: "button", onClick: () => onOpenLog("denied") } : {};
            return (<StatCard key={s.id} data-stat={s.id} {...action} className={cn("rounded-2xl border bg-white p-5 text-left shadow-[0_1px_2px_rgba(16,27,46,0.04),0_10px_30px_-14px_rgba(16,27,46,0.12)]", s.alert
                    ? "border-rose-200/80 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_2px_4px_rgba(16,27,46,0.05),0_16px_36px_-14px_rgba(16,27,46,0.18)]"
                    : "border-slate-200/80")}>
              <div className="flex items-start justify-between">
                <span className={cn("flex size-10 items-center justify-center rounded-xl", style.tint)}>
                  <Icon className="size-[18px]" strokeWidth={2.2}/>
                </span>
                {s.alert && <ArrowUpRight className="size-4 text-rose-500"/>}
              </div>
              <p className="mt-4 font-display text-[30px] font-bold leading-none tracking-tight text-navy-900">
                {s.value}
              </p>
              <p className="mt-1.5 text-[13px] font-medium text-slate-600 font-sans">{s.label}</p>
              <p className="mt-2.5 flex items-center gap-1.5 text-[12px] text-slate-600 font-sans">
                <span className={cn("size-1.5 rounded-full", style.dot, s.alert && "status-pulse")}/>
                {s.sub}
              </p>
            </StatCard>);
        })}
      </div>

      {/* perimeter status */}
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <div>
            <h2 className="font-display text-[17px] font-semibold text-navy-900">Perimeter Status</h2>
            <p className="mt-0.5 text-[12px] text-slate-600 font-sans">
              Conceptual layout — not a geographic map
            </p>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden items-center gap-3 md:flex">
              {["open", "controlled", "restricted", "critical"].map((l) => (<span key={l} className="flex items-center gap-1.5 font-sans text-[12px] font-medium text-slate-600">
                  <span className="size-2 rounded-sm" style={{ backgroundColor: LEVEL_META[l].solid }}/>
                  {LEVEL_META[l].label}
                </span>))}
            </div>
            <button onClick={() => onOpenZones()} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3.5 py-1.5 text-[13px] font-semibold text-navy-900 transition-all hover:border-primary-200 hover:bg-primary-50 hover:text-primary-700 font-sans">
              Perimeter & Zones
              <ArrowUpRight className="size-3.5"/>
            </button>
          </div>
        </div>

        <div className="grid lg:grid-cols-[minmax(0,1fr)_300px]">
          <div className="bg-dotgrid p-4">
            <PerimeterMap compact selected={null} onSelect={(zone) => onOpenZones(zone)}/>
            <p className="-mt-1 pb-1 text-center font-mono text-[9.5px] uppercase tracking-[0.16em] text-slate-600">
              Select a zone to open detail view
            </p>
          </div>

          <div className="flex flex-col border-t border-slate-100 lg:border-l lg:border-t-0">
            <div className="px-5 pt-5">
              <Eyebrow>Access points</Eyebrow>
            </div>
            <div className="flex-1 divide-y divide-slate-100 px-5">
              {gates.length === 0 && (<p className="py-6 text-center text-[12.5px] text-slate-600">
                  {isLoadingOverview ? "Loading access points..." : !error && "No access points are defined."}
                </p>)}
              {gates.map((g) => (<div key={g.id} className="flex items-center gap-3 py-3.5">
                  <span className="flex size-8 items-center justify-center rounded-lg border" style={{
                backgroundColor: levelMeta(g.zone).fill,
                borderColor: `${levelMeta(g.zone).solid}30`,
                color: levelMeta(g.zone).solid,
            }}>
                    <DoorOpen className="size-3.5" strokeWidth={2.2}/>
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12.5px] font-semibold text-navy-900">{g.name}</p>
                    <p className="truncate text-[10.5px] text-slate-600">{g.detail}</p>
                  </div>
                  <StatusChip status={g.status} size="sm"/>
                </div>))}
            </div>
            <div className="px-5 pb-5 pt-1">
              <p className="rounded-xl bg-slate-50 px-4 py-3.5 text-[11.5px] leading-relaxed text-slate-600">
                Perimeter integrity is unknown: no fence sensors are connected.
              </p>
            </div>
          </div>
        </div>
      </Card>

      {/* recent activity */}
      <Card>
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Recent Access Activity</h2>
          <button onClick={() => onOpenLog()} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-[12px] font-semibold text-navy-900 transition-all hover:border-accent-200 hover:bg-accent-50 hover:text-accent-600">
            View full log
            <ArrowUpRight className="size-3.5"/>
          </button>
        </div>
        <div className="divide-y divide-slate-100 px-6">
          {feed.length === 0 && (<p className="py-8 text-center text-[13px] text-slate-600">
              {isLoadingLogs ? "Loading recent activity..." : !error && "No access events have been recorded."}
            </p>)}
          {feed.map((e) => {
            const Icon = FEED_ICON[e.type] ?? UserRound;
            return (<button key={e.id} onClick={() => onOpenLog()} className="flex w-full items-center gap-4 py-3.5 text-left transition-colors hover:bg-slate-50/50">
                <span className={cn("flex size-9 shrink-0 items-center justify-center rounded-xl", FEED_TINT[e.type])}>
                  <Icon className="size-4" strokeWidth={2.2}/>
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
                <StatusChip status={e.status} size="sm"/>
                <span className="w-12 text-right font-mono text-[11.5px] font-medium text-slate-600">
                  {e.time}
                </span>
              </button>);
        })}
        </div>
      </Card>
    </div>);
}
