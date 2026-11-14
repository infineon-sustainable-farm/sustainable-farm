import { Card, StatusChip } from "./ui";
import EmailStateLine from "./EmailStateLine";
import { ACTION_LABEL, formatDate, formatInstant, userActions, userStatusLabel } from "../utils/userStatus";
import { CATEGORIES, permissionsSummary, zonesSummary } from "../utils/userForm";
import { cn } from "../utils/cn";

const DANGER_ACTIONS = ["revoke", "disable"];

function Dated({ label, value }) {
    return (<p className="whitespace-nowrap">
      <span className="text-slate-500">{label} </span>
      <span className="font-mono text-[11px] text-navy-900">{formatInstant(value)}</span>
    </p>);
}

/** Users table; onAction(actionId, user) is handled by the screen. */
export default function UsersTable({ users, isLoading, onAction }) {
    return (<Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[1100px] text-left font-sans text-[12.5px]">
          <thead className="border-b border-slate-100 bg-slate-50/60 text-[11px] uppercase tracking-wider text-slate-600">
            <tr>
              <th scope="col" className="px-5 py-3 font-medium">User</th>
              <th scope="col" className="px-4 py-3 font-medium">Role · category</th>
              <th scope="col" className="px-4 py-3 font-medium">Access</th>
              <th scope="col" className="px-4 py-3 font-medium">Status</th>
              <th scope="col" className="px-4 py-3 font-medium">Invitation</th>
              <th scope="col" className="px-4 py-3 font-medium">Email</th>
              <th scope="col" className="px-4 py-3 font-medium">Activity</th>
              <th scope="col" className="px-5 py-3 text-right font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 align-top">
            {users.length === 0 && (<tr>
                <td colSpan={8} className="px-6 py-8 text-center text-[13px] text-slate-600">{isLoading ? "Loading users…" : "No users yet. Invite the first one."}</td>
              </tr>)}
            {users.map((u) => (<tr key={u.id}>
                <td className="px-5 py-3.5">
                  <p className="font-semibold text-navy-900">{u.fullName || "—"}</p>
                  <p className="text-[12px] text-slate-600">{u.email}</p>
                </td>
                <td className="px-4 py-3.5 text-slate-600">
                  <p className="font-medium capitalize text-navy-900">{u.role}</p>
                  <p>{CATEGORIES.find((c) => c.value === u.category)?.label ?? u.category ?? "—"}</p>
                </td>
                <td className="px-4 py-3.5 text-slate-600">
                  <p>{permissionsSummary(u)}</p>
                  <p className="text-[11.5px]">{zonesSummary(u)}</p>
                  <p className="text-[11.5px]">Valid until {formatDate(u.validUntil)}</p>
                </td>
                <td className="px-4 py-3.5">
                  <StatusChip status={userStatusLabel(u.status)} size="sm"/>
                  {u.legacyCodeSignIn && <p className="mt-1 text-[11px] text-amber-700">Signs in with email code</p>}
                </td>
                <td className="px-4 py-3.5 text-[12px] text-slate-600">
                  <Dated label="Invited" value={u.invitedAt}/>
                  {u.invitedBy && <p className="truncate">by {u.invitedBy}</p>}
                  {u.invitationExpiresAt && ["pending", "activation_started"].includes(u.status) && <Dated label="Expires" value={u.invitationExpiresAt}/>}
                </td>
                <td className="px-4 py-3.5"><EmailStateLine delivery={u.emailDelivery} compact/></td>
                <td className="px-4 py-3.5 text-[12px]">
                  <Dated label="Activated" value={u.activatedAt}/>
                  <Dated label="First sign-in" value={u.firstLoginAt}/>
                  <Dated label="Last sign-in" value={u.lastLoginAt}/>
                </td>
                <td className="px-5 py-3.5">
                  <div className="flex max-w-[220px] flex-wrap justify-end gap-1.5 ml-auto">
                    {userActions(u).map((action) => (<button key={action} type="button" onClick={() => onAction(action, u)} aria-label={`${ACTION_LABEL[action]}: ${u.fullName || u.email}`} className={cn("whitespace-nowrap rounded-lg border px-2.5 py-1 text-[11.5px] font-semibold transition-colors", DANGER_ACTIONS.includes(action) ? "border-rose-200 text-rose-700 hover:bg-rose-50" : "border-slate-200 text-navy-900 hover:bg-slate-50")}>
                        {ACTION_LABEL[action]}
                      </button>))}
                  </div>
                </td>
              </tr>))}
          </tbody>
        </table>
      </div>
    </Card>);
}
