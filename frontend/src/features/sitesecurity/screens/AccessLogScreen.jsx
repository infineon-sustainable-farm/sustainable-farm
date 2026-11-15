import { useState } from "react";
import { ChevronRight, LogIn, LogOut } from "lucide-react";
import LogDrawer from "../components/LogDrawer";
import { Avatar, Card, FilterPill, LevelChip, ScreenHeader, StatusChip } from "../components/ui";
import { cn } from "../utils/cn";
import { filterByZone } from "../utils/accessLogs";
import { useAccessLogs } from "../hooks/useAccessLogs";
const FILTERS = [
    { id: "all", label: "All" },
    { id: "approved", label: "Approved" },
    { id: "denied", label: "Denied" },
    { id: "visitors", label: "Visitors" },
    { id: "staff", label: "Staff" },
    { id: "service", label: "Service" },
];
export default function AccessLogScreen({ initialFilter = "all", initialZone = null }) {
    const { filter, setFilter, logs, allLogs, counts, isLoading, error } = useAccessLogs(initialFilter);
    const [zone, setZone] = useState(initialZone);
    const [selected, setSelected] = useState(null);
    const rows = filterByZone(logs, zone);
    return (<div className="space-y-5">
      <ScreenHeader kicker="04 · Audit trail" title="Access Log" subtitle="Every entry, exit and refusal — in order" actions={<span className="font-mono text-[9.5px] uppercase tracking-[0.16em] text-slate-600">
            Recording simulated — prototype data
          </span>}/>

      {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
        {error}
      </div>)}

      <div className="flex flex-wrap items-center gap-2">
        {FILTERS.map((f) => (<FilterPill key={f.id} active={filter === f.id} onClick={() => setFilter(f.id)} count={counts[f.id]}>
            {f.label}
          </FilterPill>))}
        {zone && (<span className="inline-flex items-center gap-2">
            <LevelChip level={zone} size="sm"/>
            <button type="button" onClick={() => setZone(null)} className="text-[12px] font-semibold text-slate-600 underline-offset-2 hover:text-navy-900 hover:underline font-sans">
              Show all zones
            </button>
          </span>)}
        <span className="ml-auto font-mono text-[10.5px] text-slate-600">
          {rows.length} of {allLogs.length} events
        </span>
      </div>

      <Card className="overflow-hidden">
        <table className="w-full font-sans text-[13px]">
          <thead>
            <tr className="text-left font-sans text-[12px] font-medium uppercase tracking-wider text-slate-600">
              <th scope="col" className="px-6 py-3 font-medium">Time</th>
              <th scope="col" className="px-4 py-3 font-medium">User</th>
              <th scope="col" className="px-4 py-3 font-medium">Type</th>
              <th scope="col" className="px-4 py-3 font-medium">Zone</th>
              <th scope="col" className="px-4 py-3 font-medium">Action</th>
              <th scope="col" className="px-4 py-3 font-medium">Status</th>
              <th scope="col" className="w-12 px-4 py-3"><span className="sr-only">Details</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((e) => {
            const ActionIcon = e.action === "Entry" ? LogIn : LogOut;
            return (<tr key={e.id} onClick={() => setSelected(e)} className={cn("cursor-pointer border-t border-slate-100 transition-colors hover:bg-slate-50/80", e.status === "Denied" && "bg-rose-50/40 hover:bg-rose-50/60")}>
                  <td className="px-6 py-3.5">
                    <span className="font-mono text-[13px] font-semibold tabular-nums text-navy-900">
                      {e.time}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-3">
                      <Avatar initials={e.initials} level={e.type === "Unknown" ? undefined : e.zone}/>
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-semibold text-navy-900 font-sans">{e.user}</p>
                        <p className="font-mono text-[11px] text-slate-600">{e.ref}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3.5">
                    <span className={cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-[12px] font-medium font-sans", e.type === "Unknown"
                    ? "border-slate-200 bg-slate-100 text-slate-600"
                    : "border-slate-200 bg-white text-slate-600")}>
                      {e.type}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-2">
                      <LevelChip level={e.zone} size="sm"/>
                    </div>
                  </td>
                  <td className="px-4 py-3.5">
                    <span className="inline-flex items-center gap-1.5 text-[13px] font-normal text-slate-600 font-sans">
                      <ActionIcon className={cn("size-3.5", e.action === "Entry" ? "text-slate-600" : "text-slate-300")}/>
                      {e.action}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <StatusChip status={e.status} size="sm"/>
                  </td>
                  <td className="px-4 py-3.5">
                    <button type="button" aria-label={`Open event ${e.ref}`} onClick={(click) => {
                    click.stopPropagation();
                    setSelected(e);
                }} className="rounded-lg p-1 text-slate-600 transition-colors hover:bg-slate-100 hover:text-navy-900">
                      <ChevronRight className="size-4"/>
                    </button>
                  </td>
                </tr>);
        })}
            {isLoading && rows.length === 0 && (<tr>
                <td colSpan={7} className="px-6 py-12 text-center text-[13px] text-slate-600">
                  Loading events...
                </td>
              </tr>)}
            {!isLoading && !error && rows.length === 0 && (<tr>
                <td colSpan={7} className="px-6 py-12 text-center text-[13px] text-slate-600">
                  No events match this filter.
                </td>
              </tr>)}
          </tbody>
        </table>
      </Card>

      <LogDrawer entry={selected} onClose={() => setSelected(null)}/>
    </div>);
}
