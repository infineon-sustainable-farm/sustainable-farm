import { useState } from "react";
import { HardHat, Plus, ShieldCheck, UserRound, Wrench } from "lucide-react";
import AddAccessModal from "../components/AddAccessModal";
import {
  Avatar,
  Card,
  LevelChip,
  ScreenHeader,
  SecurityLadder,
  SensitivityMeter,
  StatusChip,
} from "../components/ui";
import { cn } from "../utils/cn";
import type { User, UserType } from "../api/sitesecurityTypes";
import { useCredentials } from "../hooks/useCredentials";

const TYPE_ICON: Record<UserType, typeof UserRound> = {
  Staff: HardHat,
  Visitor: UserRound,
  Service: Wrench,
  Security: ShieldCheck,
};

export default function AccessControl() {
  const { users, freshId, addCredential } = useCredentials();
  const [open, setOpen] = useState(false);

  const addUser = (u: User) => {
    addCredential(u);
  };

  return (
    <div className="space-y-5">
      <ScreenHeader
        kicker="02 · Credentials"
        title="Access Control"
        subtitle="Who is allowed where — and until when"
        actions={
          <button
            onClick={() => setOpen(true)}
            className="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-[13px] font-semibold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-primary-700 font-sans"
          >
            <Plus className="size-4" strokeWidth={2.5} />
            Add Access
          </button>
        }
      />

      <SecurityLadder />

      <Card className="overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <h2 className="font-display text-[17px] font-semibold text-navy-900">Credential Holders</h2>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 font-mono text-[11px] font-semibold text-slate-500">
              {users.length}
            </span>
          </div>
          <p className="font-sans text-[12px] text-slate-400">
            Prototype data — no directory connected
          </p>
        </div>
        <table className="w-full border-t border-slate-100 font-sans text-[13px]">
          <thead>
            <tr className="text-left font-sans text-[12px] font-medium uppercase tracking-wider text-slate-400">
              <th className="px-6 py-3 font-medium">Name</th>
              <th className="px-4 py-3 font-medium">Type</th>
              <th className="px-4 py-3 font-medium">Access Level</th>
              <th className="hidden px-4 py-3 font-medium xl:table-cell">Valid Until</th>
              <th className="hidden px-4 py-3 font-medium lg:table-cell">Last Activity</th>
              <th className="px-6 py-3 text-right font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const TypeIcon = TYPE_ICON[u.type];
              return (
                <tr
                  key={u.id}
                  className={cn(
                    "border-t border-slate-100 transition-colors hover:bg-slate-50/70",
                    u.id === freshId && "row-in"
                  )}
                >
                  <td className="px-6 py-3.5">
                    <div className="flex items-center gap-3">
                      <Avatar initials={u.initials} level={u.level} />
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-semibold text-navy-900 font-sans">{u.name}</p>
                        <p className="truncate text-[12px] text-slate-400 font-sans">{u.role}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3.5">
                    <span className="inline-flex items-center gap-1.5 text-[13px] font-normal text-slate-600 font-sans">
                      <TypeIcon className="size-3.5 text-slate-400" />
                      {u.type}
                    </span>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-2.5">
                      <SensitivityMeter level={u.level} />
                      <LevelChip level={u.level} size="sm" />
                    </div>
                  </td>
                  <td className="hidden px-4 py-3.5 xl:table-cell">
                    <span className="font-sans text-[13px] text-slate-500">{u.validUntil}</span>
                  </td>
                  <td className="hidden px-4 py-3.5 lg:table-cell">
                    <span className="font-sans text-[13px] text-slate-400">{u.lastActive}</span>
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <StatusChip status={u.status} size="sm" />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </Card>

      <AddAccessModal open={open} onClose={() => setOpen(false)} onAdd={addUser} />
    </div>
  );
}
