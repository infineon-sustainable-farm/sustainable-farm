import { ArrowUpRight, HardHat, ShieldAlert, ShieldCheck, Truck, UserRound } from "lucide-react";
import { cn } from "../utils/cn";
import { Card, StatusChip } from "./ui";
import { useAccessLogs } from "../hooks/useAccessLogs";
import { recentEvents } from "../utils/accessLogs";

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

/* Rendered only for accounts allowed to read the access log. */
export default function RecentActivity({ onOpenLog }) {
    const { allLogs, isLoading, error } = useAccessLogs();
    const feed = recentEvents(allLogs);
    return (<Card>
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Recent Access Activity</h2>
          <button onClick={() => onOpenLog()} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-[12px] font-semibold text-navy-900 transition-all hover:border-accent-200 hover:bg-accent-50 hover:text-accent-600">
            View full log
            <ArrowUpRight className="size-3.5"/>
          </button>
        </div>
        {error && (<div role="alert" className="mx-6 mt-4 rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
          {error}
        </div>)}
        <div className="divide-y divide-slate-100 px-6">
          {feed.length === 0 && (<p className="py-8 text-center text-[13px] text-slate-600">
              {isLoading ? "Loading recent activity..." : !error && "No access events have been recorded."}
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
      </Card>);
}
