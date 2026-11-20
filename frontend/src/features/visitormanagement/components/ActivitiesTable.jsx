import { useState } from "react";
import { Pencil, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { ConfirmDialog, IconButton, Table } from "../../../shared/ui";
import StatusTag from "./StatusTag";
import { useVisitorFormat } from "../utils/format";

/**
 * The activities and their actions. Deactivation is irreversible through the API (there is no
 * active flag on the update payload) and cancels the activity's bookings, so it asks for
 * confirmation and spells the consequence out.
 */
export default function ActivitiesTable({ activities, pendingDeactivateId, deactivateError, onDeactivate, onEdit }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const [confirming, setConfirming] = useState(null);

    const columns = [
        {
            key: "name",
            header: t("booking.activities.table.activity"),
            cell: (activity) => (
                <>
                    {activity.name}
                    {activity.description && (
                        <span className="mt-0.5 block text-xs font-normal text-text-muted">{activity.description}</span>
                    )}
                </>
            ),
        },
        {
            key: "price",
            header: t("booking.activities.table.price"),
            className: "whitespace-nowrap",
            cell: (activity) => format.fcfa(activity.price),
        },
        {
            key: "capacity",
            header: t("booking.activities.table.capacity"),
            cell: (activity) => format.number(activity.capacity),
        },
        {
            key: "durationMinutes",
            header: t("booking.activities.table.duration"),
            className: "whitespace-nowrap",
            cell: (activity) => t("booking.activities.table.minutes", { value: activity.durationMinutes }),
        },
        {
            key: "status",
            header: t("booking.activities.table.status"),
            cell: (activity) => <StatusTag value={activity.active ? "ACTIVE" : "INACTIVE"} />,
        },
    ];

    return (
        <>
            <Table
                caption={t("booking.activities.title")}
                columns={columns}
                rows={activities}
                emptyMessage={t("booking.activities.table.empty")}
                rowActions={(activity) => {
                    const isPending = pendingDeactivateId === activity.id;
                    const failed = deactivateError?.id === activity.id;
                    return (
                        <div className="flex flex-col gap-1">
                            <div className="flex items-center gap-1">
                                <IconButton
                                    icon={Pencil}
                                    label={t("booking.activities.actions.edit")}
                                    disabled={isPending}
                                    onClick={() => onEdit(activity)}
                                />
                                {activity.active && (
                                    <IconButton
                                        icon={X}
                                        tone="danger"
                                        label={
                                            isPending
                                                ? t("booking.activities.actions.deactivating")
                                                : t("booking.activities.actions.deactivate")
                                        }
                                        aria-busy={isPending || undefined}
                                        disabled={isPending}
                                        onClick={() => setConfirming(activity)}
                                    />
                                )}
                            </div>
                            {failed && <p className="max-w-xs text-xs text-error-fg">{deactivateError.message}</p>}
                        </div>
                    );
                }}
            />
            <ConfirmDialog
                open={Boolean(confirming)}
                title={t("booking.activities.confirm.title")}
                message={t("booking.activities.note")}
                confirmLabel={t("booking.activities.confirm.yes")}
                cancelLabel={t("booking.activities.confirm.no")}
                onClose={() => setConfirming(null)}
                onConfirm={() => {
                    onDeactivate(confirming);
                    setConfirming(null);
                }}
            />
        </>
    );
}
