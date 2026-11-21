import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Ban, Plus } from "lucide-react";
import { Button, ConfirmDialog, ErrorState, IconButton, PageHeader, Table, useToast } from "../../../shared/ui";
import ScreenAccess from "../components/ScreenAccess";
import AddAccessModal from "../components/AddAccessModal";
import { Avatar, InlineAlert, LevelBadge, SecurityLadder, SensitivityMeter, StatusLabel } from "../components/ui";
import { useCredentials } from "../hooks/useCredentials";
import { useSiteSecurityAccess } from "../hooks/useSiteSecurityAccess";
import { isAdmin } from "../utils/accessRights";
import { canRevoke, emailStatusKey } from "../utils/credentialForm";
import { translateEnum } from "../utils/status";

function AccessControlContent() {
  const { t } = useTranslation("sitesecurity");
  const toast = useToast();
  const { authorization } = useSiteSecurityAccess();
  const canManage = isAdmin(authorization);
  const { users, addCredential, revokeCredential, isLoading, error } = useCredentials();
  const [open, setOpen] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [revokeError, setRevokeError] = useState(null);
  const [isRevoking, setIsRevoking] = useState(false);
  const failedEmails = canManage ? users.filter((u) => u.notificationStatus === "failed").length : 0;

  const add = async (payload) => {
    await addCredential(payload);
    toast.success(t("access.created"));
  };

  const confirmRevoke = async () => {
    setIsRevoking(true);
    setRevokeError(null);
    try {
      await revokeCredential(revokeTarget.id);
      toast.success(t("access.revoked"));
      setRevokeTarget(null);
    } catch (err) {
      setRevokeError(err.message || t("access.revokeError"));
    } finally {
      setIsRevoking(false);
    }
  };

  const columns = [
    {
      key: "name",
      header: t("access.columns.holder"),
      cell: (u) => (
        <div className="flex items-center gap-3">
          <Avatar initials={u.initials} level={u.level} />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-text">{u.name ?? t("access.unknownPerson", { id: u.personId })}</p>
            <p className="truncate text-xs text-text-muted">{u.role}</p>
            {canManage && u.email && <p className="truncate text-xs text-text-muted">{u.email}</p>}
          </div>
        </div>
      ),
    },
    { key: "type", header: t("access.columns.type"), cell: (u) => translateEnum(t, "type", u.type) },
    {
      key: "level",
      header: t("access.columns.level"),
      cell: (u) => (
        <span className="flex items-center gap-2">
          <SensitivityMeter level={u.level} />
          <LevelBadge level={u.level} />
        </span>
      ),
    },
    {
      key: "validUntil",
      header: t("access.columns.validUntil"),
      cell: (u) => (u.validUntil === "Permanent" ? t("access.permanent") : u.validUntil),
    },
    { key: "lastActive", header: t("access.columns.lastActive") },
    {
      key: "status",
      header: t("access.columns.status"),
      cell: (u) => (
        <div>
          <StatusLabel status={u.status} />
          {canManage && emailStatusKey(u) && <p className="mt-1 text-xs text-text-muted">{t(emailStatusKey(u))}</p>}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5 p-6">
      <PageHeader
        title={t("access.title")}
        subtitle={t("access.subtitle")}
        actions={
          canManage && (
            <Button icon={Plus} onClick={() => setOpen(true)}>
              {t("access.add")}
            </Button>
          )
        }
      />

      {error && <ErrorState message={t("access.loadError")} />}
      {failedEmails > 0 && <InlineAlert tone="warning">{t("access.emailsFailed", { count: failedEmails })}</InlineAlert>}

      <SecurityLadder />

      <Table
        caption={t("access.tableCaption")}
        columns={columns}
        rows={users}
        getRowKey={(u) => u.id}
        loading={isLoading}
        emptyMessage={t("access.empty")}
        rowActions={
          canManage
            ? (u) =>
                canRevoke(u) && (
                  <IconButton
                    icon={Ban}
                    tone="danger"
                    label={t("access.revokeFor", { name: u.name ?? u.personId })}
                    onClick={() => {
                      setRevokeError(null);
                      setRevokeTarget(u);
                    }}
                  />
                )
            : undefined
        }
      />

      {canManage && <AddAccessModal open={open} onClose={() => setOpen(false)} onAdd={add} />}
      <ConfirmDialog
        open={Boolean(revokeTarget)}
        onClose={() => setRevokeTarget(null)}
        onConfirm={confirmRevoke}
        title={t("access.revokeTitle")}
        message={revokeTarget ? t("access.revokeMessage", { name: revokeTarget.name ?? revokeTarget.personId }) : ""}
        confirmLabel={t("access.revoke")}
        variant="danger"
        loading={isRevoking}
        error={revokeError}
      />
    </div>
  );
}

export default function AccessControl() {
  return (
    <ScreenAccess screen="access">
      <AccessControlContent />
    </ScreenAccess>
  );
}
