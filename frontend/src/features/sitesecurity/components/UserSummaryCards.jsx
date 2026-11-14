import { cn } from "../utils/cn";
import { summaryCards } from "../utils/userStatus";

export default function UserSummaryCards({ summary, compact = false }) {
    return (<div className={cn("grid gap-4", compact ? "grid-cols-2 lg:grid-cols-4" : "grid-cols-2 xl:grid-cols-4")}>
      {summaryCards(summary).map((card) => (<div key={card.id} className={cn("rounded-2xl border border-slate-200/80 bg-white", compact ? "p-4" : "p-5 shadow-[0_1px_2px_rgba(16,27,46,0.04),0_10px_30px_-14px_rgba(16,27,46,0.12)]")}>
          <span className={cn("inline-flex rounded-full px-2.5 py-0.5 font-sans text-[11.5px] font-semibold", card.tone)}>{card.label}</span>
          <p className={cn("mt-3 font-display font-bold leading-none tracking-tight text-navy-900", compact ? "text-[24px]" : "text-[30px]")}>
            {summary ? card.value : "—"}
          </p>
          <p className="mt-1.5 font-sans text-[12px] text-slate-600">{card.sub}</p>
        </div>))}
    </div>);
}
