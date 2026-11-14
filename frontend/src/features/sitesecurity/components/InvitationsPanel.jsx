import { ArrowUpRight } from "lucide-react";
import { useUsersSummary } from "../hooks/useUsers";
import { Card } from "./ui";
import UserSummaryCards from "./UserSummaryCards";

/** Admin-only dashboard panel: invitation counts and a way to the Users screen. */
export default function InvitationsPanel({ onOpenUsers }) {
    const { summary, error } = useUsersSummary();
    return (<Card>
      <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
        <div>
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Invitations</h2>
          <p className="mt-0.5 font-sans text-[12px] text-slate-600">Accounts of the people who use Site Security</p>
        </div>
        <button type="button" onClick={onOpenUsers} className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3.5 py-1.5 font-sans text-[13px] font-semibold text-navy-900 transition-all hover:border-primary-200 hover:bg-primary-50 hover:text-primary-700">
          Open Users
          <ArrowUpRight className="size-3.5"/>
        </button>
      </div>
      <div className="p-5">
        {error ? (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">{error}</div>) : (<UserSummaryCards summary={summary} compact/>)}
      </div>
    </Card>);
}
