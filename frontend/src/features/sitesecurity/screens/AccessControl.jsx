import { useEffect, useRef, useState } from "react";
import { HardHat, Plus, ShieldCheck, UserRound, Wrench } from "lucide-react";
import AddAccessModal from "../components/AddAccessModal";
import { Avatar, Card, LevelChip, ScreenHeader, SecurityLadder, SensitivityMeter, StatusChip, } from "../components/ui";
import { cn } from "../utils/cn";
import { canRevoke } from "../utils/credentialForm";
import { useCredentials } from "../hooks/useCredentials";
const TYPE_ICON = {
    Staff: HardHat,
    Visitor: UserRound,
    Service: Wrench,
    Security: ShieldCheck,
};
function RevokeAction({ user, isConfirming, onAsk, onConfirm, onCancel }) {
    const askRef = useRef(null);
    const confirmRef = useRef(null);
    const wasConfirming = useRef(false);
    /* The pressed button disappears, so focus has to move with the choice. */
    useEffect(() => {
        if (isConfirming)
            confirmRef.current?.focus();
        else if (wasConfirming.current)
            askRef.current?.focus();
        wasConfirming.current = isConfirming;
    }, [isConfirming]);
    if (!canRevoke(user))
        return null;
    if (!isConfirming) {
        return (<button ref={askRef} type="button" aria-label={`Revoke access for ${user.name}`} onClick={onAsk} className="rounded-lg border border-slate-200 px-2.5 py-1 text-[12px] font-semibold text-slate-600 transition-colors hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700 font-sans">
        Revoke
      </button>);
    }
    return (<span className="inline-flex items-center gap-1.5">
      <button ref={confirmRef} type="button" aria-label={`Confirm revoking access for ${user.name}`} onClick={onConfirm} className="rounded-lg bg-rose-600 px-2.5 py-1 text-[12px] font-semibold text-white transition-colors hover:bg-rose-700 font-sans">
        Confirm
      </button>
      <button type="button" aria-label={`Keep access for ${user.name}`} onClick={onCancel} className="rounded-lg border border-slate-200 px-2.5 py-1 text-[12px] font-semibold text-slate-600 transition-colors hover:bg-slate-50 font-sans">
        Keep
      </button>
    </span>);
}
export default function AccessControl({ canManage = false }) {
    const { users, freshId, addCredential, revokeCredential, isLoading, error } = useCredentials();
    const [open, setOpen] = useState(false);
    const [confirmingId, setConfirmingId] = useState(null);
    const headingRef = useRef(null);
    const columnCount = canManage ? 7 : 6;
    const confirmRevoke = async (id) => {
        await revokeCredential(id);
        setConfirmingId(null);
        /* The row has no action left, so focus goes to a stable place. */
        headingRef.current?.focus();
    };
    return (<div className="space-y-5">
      <ScreenHeader kicker="02 · Credentials" title="Access Control" subtitle="Who is allowed where — and until when" actions={canManage && (<button onClick={() => setOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-[13px] font-semibold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-primary-700 font-sans">
            <Plus className="size-4" strokeWidth={2.5}/>
            Add Access
          </button>)}/>

      {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
        {error}
      </div>)}

      <SecurityLadder />

      <Card className="overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <h2 ref={headingRef} tabIndex={-1} className="font-display text-[17px] font-semibold text-navy-900 outline-none">Credential Holders</h2>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 font-mono text-[11px] font-semibold text-slate-600">
              {users.length}
            </span>
          </div>
          <p className="font-sans text-[12px] text-slate-600">
            Prototype data — no directory connected
          </p>
        </div>
        <table className="w-full border-t border-slate-100 font-sans text-[13px]">
          <thead>
            <tr className="text-left font-sans text-[12px] font-medium uppercase tracking-wider text-slate-600">
              <th scope="col" className="px-6 py-3 font-medium">Name</th>
              <th scope="col" className="px-4 py-3 font-medium">Type</th>
              <th scope="col" className="px-4 py-3 font-medium">Access Level</th>
              <th scope="col" className="hidden px-4 py-3 font-medium xl:table-cell">Valid Until</th>
              <th scope="col" className="hidden px-4 py-3 font-medium lg:table-cell">Last Activity</th>
              <th scope="col" className="px-6 py-3 text-right font-medium">Status</th>
              {canManage && <th scope="col" className="px-6 py-3 text-right font-medium">Action</th>}
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
            const TypeIcon = TYPE_ICON[u.type] ?? UserRound;
            return (<tr key={u.id} className={cn("border-t border-slate-100 transition-colors hover:bg-slate-50/70", u.id === freshId && "row-in")}>
                  <td className="px-6 py-3.5">
                    <div className="flex items-center gap-3">
                      <Avatar initials={u.initials} level={u.level}/>
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-semibold text-navy-900 font-sans">{u.name}</p>
                        <p className="truncate text-[12px] text-slate-600 font-sans">{u.role}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3.5">
                    <span className="inline-flex items-center gap-1.5 text-[13px] font-normal text-slate-600 font-sans">
                      <TypeIcon className="size-3.5 text-slate-600"/>
                      {u.type}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-2.5">
                      <SensitivityMeter level={u.level}/>
                      <LevelChip level={u.level} size="sm"/>
                    </div>
                  </td>
                  <td className="hidden px-4 py-3.5 xl:table-cell">
                    <span className="font-sans text-[13px] text-slate-600">{u.validUntil}</span>
                  </td>
                  <td className="hidden px-4 py-3.5 lg:table-cell">
                    <span className="font-sans text-[13px] text-slate-600">{u.lastActive}</span>
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <StatusChip status={u.status} size="sm"/>
                  </td>
                  {canManage && (<td className="px-6 py-3.5 text-right">
                      <RevokeAction user={u} isConfirming={confirmingId === u.id} onAsk={() => setConfirmingId(u.id)} onConfirm={() => confirmRevoke(u.id)} onCancel={() => setConfirmingId(null)}/>
                    </td>)}
                </tr>);
        })}
            {users.length === 0 && (<tr>
                <td colSpan={columnCount} className="px-6 py-12 text-center text-[13px] text-slate-600">
                  {isLoading ? "Loading credentials..." : !error && "No credentials have been granted yet."}
                </td>
              </tr>)}
          </tbody>
        </table>
      </Card>

      {canManage && <AddAccessModal open={open} onClose={() => setOpen(false)} onAdd={addCredential}/>}
    </div>);
}
