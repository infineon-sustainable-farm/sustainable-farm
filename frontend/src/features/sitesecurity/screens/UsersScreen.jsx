import { useState } from "react";
import { UserPlus } from "lucide-react";
import { disableUser, enableUser, resendInvitation, revokeInvitation } from "../api/sitesecurityApi";
import { useUsers } from "../hooks/useUsers";
import { ScreenHeader } from "../components/ui";
import { ErrorAlert, SuccessNote } from "../components/formUi";
import { PRIMARY_BUTTON } from "../utils/formClasses";
import UserSummaryCards from "../components/UserSummaryCards";
import UsersTable from "../components/UsersTable";
import CodeSignInCard from "../components/CodeSignInCard";
import InviteUserModal from "../components/InviteUserModal";
import EditUserModal from "../components/EditUserModal";
import ConfirmActionDialog from "../components/ConfirmActionDialog";
import ActivationLinkDialog from "../components/ActivationLinkDialog";
import ConfirmDeliveryDialog from "../components/ConfirmDeliveryDialog";
import UserHistoryDrawer from "../components/UserHistoryDrawer";

function nameOf(user) {
    return user.fullName || user.email;
}

/** Admin-only screen: invitations, accounts, emails and the legacy sign-in switch. */
export default function UsersScreen() {
    const { users, summary, isLoading, error, replaceUser, reloadUsers } = useUsers();
    const [inviting, setInviting] = useState(false);
    const [editing, setEditing] = useState(null);
    const [confirming, setConfirming] = useState(null);
    const [linkFor, setLinkFor] = useState(null);
    const [deliveryTarget, setDeliveryTarget] = useState(null);
    const [historyFor, setHistoryFor] = useState(null);
    const [notice, setNotice] = useState(null);
    const [actionError, setActionError] = useState(null);

    /* Resend and enable run at once; the screen reports the result. */
    const runNow = async (call, user, done) => {
        setNotice(null);
        setActionError(null);
        try {
            replaceUser(await call(user.id));
            setNotice(done);
        }
        catch (err) {
            setActionError(err.message || "The action failed.");
        }
    };

    const ask = (title, message, confirmLabel, call, user) => setConfirming({
        title,
        message,
        confirmLabel,
        danger: true,
        run: async () => replaceUser(await call(user.id)),
    });

    const onAction = (action, user) => {
        switch (action) {
            case "resend":
                return runNow(resendInvitation, user, `A new invitation was submitted for ${nameOf(user)}. The previous link no longer works.`);
            case "setupLink":
                return runNow(resendInvitation, user, `A password setup link was submitted for ${nameOf(user)}.`);
            case "enable":
                return runNow(enableUser, user, `${nameOf(user)} can sign in again.`);
            case "revoke":
                return ask("Revoke invitation", `The invitation link of ${nameOf(user)} stops working and the account cannot be activated.`, "Revoke invitation", revokeInvitation, user);
            case "disable":
                return ask("Disable account", `${nameOf(user)} is signed out and can no longer sign in until enabled again.`, "Disable", disableUser, user);
            case "copyLink":
                return setLinkFor(user);
            case "edit":
                return setEditing(user);
            case "history":
                return setHistoryFor(user);
            case "confirmDelivery":
                return setDeliveryTarget({ user, delivery: user.emailDelivery });
            default:
                return undefined;
        }
    };

    return (<div className="space-y-5">
      <ScreenHeader kicker="05 · Accounts" title="Users" subtitle="Who can sign in to Site Security, and what they can see" actions={<button type="button" onClick={() => setInviting(true)} className={PRIMARY_BUTTON}>
            <UserPlus className="size-4"/>
            Invite user
          </button>}/>

      <ErrorAlert>{error}</ErrorAlert>
      <ErrorAlert>{actionError}</ErrorAlert>
      {notice && <SuccessNote>{notice}</SuccessNote>}

      <UserSummaryCards summary={summary}/>
      <UsersTable users={users} isLoading={isLoading} onAction={onAction}/>
      <CodeSignInCard />

      <InviteUserModal open={inviting} onClose={() => setInviting(false)} onInvited={(user) => {
            replaceUser(user);
            setNotice(`Invitation submitted for ${nameOf(user)}. Submitted to the mail provider does not mean delivered.`);
        }}/>
      <EditUserModal user={editing} onClose={() => setEditing(null)} onSaved={replaceUser}/>
      <ConfirmActionDialog request={confirming} onClose={() => setConfirming(null)}/>
      <ActivationLinkDialog user={linkFor} onClose={() => setLinkFor(null)} onCreated={reloadUsers}/>
      <ConfirmDeliveryDialog target={deliveryTarget} onClose={() => setDeliveryTarget(null)} onConfirmed={reloadUsers}/>
      <UserHistoryDrawer user={historyFor} refreshSeed={0} onClose={() => setHistoryFor(null)} onConfirmDelivery={(user, delivery) => {
            setHistoryFor(null);
            setDeliveryTarget({ user, delivery });
        }}/>
    </div>);
}
