import { useState } from "react";
import { Ban, Check, CircleCheck, Pencil, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, ConfirmDialog, Table } from "../../../shared/ui";
import { useVisitorFormat } from "../utils/format";
import { useLabels } from "../utils/labels";
import StatusTag from "./StatusTag";

/*
 * Which transitions the backend accepts, per current status: approve and reject only from
 * PENDING, check-in only from CONFIRMED, and cancel while the registration is still active (the
 * API refuses after check-in). Showing any other button would guarantee a 422, so the table never
 * offers it.
 */
function rowActions(status) {
    if (status === "PENDING") return ["approve", "reject", "cancel"];
    if (status === "CONFIRMED") return ["check-in", "cancel"];
    return [];
}

const ACTIONS = {
    approve: { icon: Check, labelKey: "registration.actions.approve" },
    reject: { icon: X, labelKey: "registration.actions.reject" },
    "check-in": { icon: CircleCheck, labelKey: "registration.actions.checkIn" },
    cancel: { icon: Ban, labelKey: "registration.actions.cancel" },
};

/**
 * The "Registered visitors" table. Language and type are not part of a registration payload, so
 * they are joined from the visitors list by id; a registration without a visitor (the API allows
 * it) shows dashes.
 */
export default function RegistrationsTable({
    registrations,
    visitorsById,
    slotsById,
    eventsById,
    pendingAction,
    actionError,
    onAction,
    onEdit,
}) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
    const [cancelTarget, setCancelTarget] = useState(null);

    function slotCell(registration) {
        if (registration.eventId) {
            return t("registration.eventCell", {
                title: eventsById?.get(registration.eventId)?.title ?? `#${registration.eventId}`,
            });
        }
        const slot = slotsById.get(registration.timeSlotId);
        return t("registration.slotCell", {
            date: format.dateShort(registration.slotDate),
            time: slot ? format.timeRange(slot.startTime, slot.endTime) : format.time(registration.slotStart),
        });
    }

    const columns = [
        {
            key: "visitorName",
            header: t("registration.columns.name"),
            cell: (registration) => registration.visitorName ?? format.none,
        },
        {
            key: "groupSize",
            header: t("registration.columns.group"),
            cell: (registration) => registration.groupSize ?? format.none,
        },
        { key: "slot", header: t("registration.columns.dateSlot"), className: "whitespace-nowrap", cell: slotCell },
        {
            key: "language",
            header: t("registration.columns.language"),
            cell: (registration) => {
                const language = visitorsById.get(registration.visitorId)?.language;
                return language ? labels.language(language) : format.none;
            },
        },
        {
            key: "type",
            header: t("registration.columns.type"),
            cell: (registration) => {
                const type = visitorsById.get(registration.visitorId)?.type;
                return type ? labels.enum(type) : format.none;
            },
        },
        {
            key: "status",
            header: t("registration.columns.status"),
            cell: (registration) => <StatusTag value={registration.status} />,
        },
    ];

    function renderActions(registration) {
        const isPending = pendingAction?.id === registration.id;
        const failed = actionError?.id === registration.id;
        return (
            <div className="flex flex-col items-start gap-1">
                <div className="flex flex-wrap items-center gap-1.5">
                    {rowActions(registration.status).map((action) => {
                        const { icon, labelKey } = ACTIONS[action];
                        return (
                            <Button
                                key={action}
                                size="sm"
                                variant="secondary"
                                icon={icon}
                                disabled={isPending}
                                onClick={() =>
                                    action === "cancel" ? setCancelTarget(registration) : onAction(registration, action)
                                }
                            >
                                {t(labelKey)}
                            </Button>
                        );
                    })}
                    <Button
                        size="sm"
                        variant="ghost"
                        icon={Pencil}
                        disabled={isPending || !registration.visitorId}
                        onClick={() => onEdit(registration)}
                    >
                        {t("registration.actions.edit")}
                    </Button>
                </div>
                {failed && <p className="max-w-64 text-xs text-error-fg">{actionError.message}</p>}
            </div>
        );
    }

    return (
        <>
            <Table
                caption={t("registration.tableTitle")}
                columns={columns}
                rows={registrations}
                emptyMessage={t("registration.empty")}
                rowActions={renderActions}
            />
            <ConfirmDialog
                open={Boolean(cancelTarget)}
                title={t("registration.cancelTitle")}
                message={
                    cancelTarget
                        ? t("registration.cancelMessage", {
                              name: cancelTarget.visitorName ?? format.none,
                          })
                        : ""
                }
                confirmLabel={t("registration.confirmYes")}
                cancelLabel={t("registration.confirmNo")}
                onClose={() => setCancelTarget(null)}
                onConfirm={() => {
                    const target = cancelTarget;
                    setCancelTarget(null);
                    onAction(target, "cancel");
                }}
            />
        </>
    );
}
