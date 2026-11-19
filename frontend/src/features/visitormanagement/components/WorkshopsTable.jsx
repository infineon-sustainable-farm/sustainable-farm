import { Ban, Pencil, Send } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import StatusTag from "./StatusTag";

/*
 * The API only accepts publish on a DRAFT and deactivate on an ACTIVE workshop, so the table
 * offers exactly the transition each row allows.
 */
function statusActions(status) {
    if (status === "DRAFT") return ["publish"];
    if (status === "ACTIVE") return ["deactivate"];
    return [];
}

export default function WorkshopsTable({ workshops, pendingAction, actionError, onAction, onEdit }) {
    const { t } = useTranslation("visitormanagement");

    const columns = [
        {
            key: "name",
            header: t("education.workshops.columns.name"),
            cell: (workshop) => (
                <>
                    {workshop.name}
                    {workshop.description && (
                        <span className="mt-0.5 block text-xs font-normal text-text-muted">
                            {workshop.description}
                        </span>
                    )}
                </>
            ),
        },
        {
            key: "durationMinutes",
            header: t("education.workshops.columns.duration"),
            className: "whitespace-nowrap",
            cell: (workshop) => t("education.minutes", { value: workshop.durationMinutes }),
        },
        { key: "targetGroup", header: t("education.workshops.columns.targetGroup") },
        {
            key: "facilitator",
            header: t("education.workshops.columns.facilitator"),
            cell: (workshop) => workshop.facilitator ?? t("format.none"),
        },
        {
            key: "status",
            header: t("education.workshops.columns.status"),
            cell: (workshop) => <StatusTag value={workshop.status} />,
        },
    ];

    return (
        <Table
            caption={t("education.workshops.title")}
            columns={columns}
            rows={workshops}
            emptyMessage={t("education.workshops.empty")}
            rowActions={(workshop) => {
                const isPending = pendingAction?.id === workshop.id;
                return (
                    <div>
                        <div className="flex items-center gap-1">
                            <IconButton
                                icon={Pencil}
                                label={t("education.actions.edit")}
                                disabled={isPending}
                                onClick={() => onEdit(workshop)}
                            />
                            {statusActions(workshop.status).map((action) => (
                                <IconButton
                                    key={action}
                                    icon={action === "publish" ? Send : Ban}
                                    tone={action === "deactivate" ? "danger" : "primary"}
                                    label={
                                        action === "publish"
                                            ? t("education.actions.publish")
                                            : t("education.actions.deactivate")
                                    }
                                    disabled={isPending}
                                    aria-busy={isPending || undefined}
                                    onClick={() => onAction(workshop, action)}
                                />
                            ))}
                        </div>
                        {actionError?.id === workshop.id && (
                            <p role="alert" className="mt-1 max-w-xs text-xs text-error-fg">
                                {actionError.message}
                            </p>
                        )}
                    </div>
                );
            }}
        />
    );
}
