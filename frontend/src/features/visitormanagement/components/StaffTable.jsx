import { CircleCheck, CircleX, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, IconButton, Table } from "../../../shared/ui";
import { useLabels } from "../utils/labels";
import StatusTag from "./StatusTag";

/**
 * One row's actions. Deactivation is confirmed by the page because it removes the member from
 * every selector at once; reactivation goes through an update with active=true, the only path
 * the API offers.
 */
function StaffActions({ member, isPending, onAction, onDeactivate, onEdit }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <>
            <IconButton icon={Pencil} label={t("staff.edit")} disabled={isPending} onClick={() => onEdit(member)} />
            {member.active ? (
                <Button
                    variant="ghost"
                    size="sm"
                    icon={CircleX}
                    disabled={isPending}
                    className="text-error-fg"
                    onClick={() => onDeactivate(member)}
                >
                    {isPending ? t("staff.deactivating") : t("staff.deactivate")}
                </Button>
            ) : (
                <Button
                    variant="ghost"
                    size="sm"
                    icon={CircleCheck}
                    disabled={isPending}
                    onClick={() => onAction(member, "activate")}
                >
                    {isPending ? t("staff.activating") : t("staff.activate")}
                </Button>
            )}
        </>
    );
}

export default function StaffTable({ staff, pendingAction, actionError, onAction, onDeactivate, onEdit }) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const none = t("format.none");

    const columns = [
        { key: "fullName", header: t("staff.columns.name") },
        { key: "role", header: t("staff.columns.role"), cell: (member) => labels.enum(member.role) },
        { key: "email", header: t("staff.columns.email"), cell: (member) => member.email ?? none },
        { key: "phone", header: t("staff.columns.phone"), cell: (member) => member.phone ?? none },
        {
            key: "status",
            header: t("staff.columns.status"),
            cell: (member) => <StatusTag value={member.active ? "ACTIVE" : "INACTIVE"} />,
        },
    ];

    return (
        <Table
            caption={t("staff.title")}
            columns={columns}
            rows={staff}
            emptyMessage={t("staff.noMatch")}
            actionsHeader={t("staff.columns.actions")}
            rowActions={(member) => (
                <div className="flex flex-col items-start gap-1">
                    <div className="flex flex-wrap items-center gap-1">
                        <StaffActions
                            member={member}
                            isPending={pendingAction?.staff?.id === member.id}
                            onAction={onAction}
                            onDeactivate={onDeactivate}
                            onEdit={onEdit}
                        />
                    </div>
                    {actionError?.id === member.id && (
                        <p role="alert" className="max-w-64 text-xs text-error-fg">
                            {actionError.message}
                        </p>
                    )}
                </div>
            )}
        />
    );
}
