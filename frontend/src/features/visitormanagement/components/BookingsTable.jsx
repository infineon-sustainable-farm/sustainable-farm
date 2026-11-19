import { Banknote, CircleCheck, Check, Pencil, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import StatusTag from "./StatusTag";
import { useVisitorFormat } from "../utils/format";
import { useLabels } from "../utils/labels";

/*
 * The transitions the API accepts, per row. A booking must be PAID before it can be confirmed,
 * so an unpaid pending booking offers "pay", not "confirm": offering confirm would guarantee a
 * 422. Completing requires CONFIRMED, and cancelling is only possible while the booking is still
 * active.
 */
function rowActions(booking) {
    const actions = [];
    const active = booking.status === "PENDING" || booking.status === "CONFIRMED";
    if (active && booking.paymentStatus === "UNPAID") actions.push("pay");
    if (booking.status === "PENDING" && booking.paymentStatus === "PAID") actions.push("confirm");
    if (booking.status === "CONFIRMED") actions.push("complete");
    if (active) actions.push("cancel");
    if (booking.status !== "COMPLETED" && booking.status !== "CANCELLED") actions.push("edit");
    return actions;
}

const ACTION_ICONS = { pay: Banknote, confirm: Check, complete: CircleCheck, cancel: X, edit: Pencil };

export default function BookingsTable({ bookings, pendingAction, actionError, onAction, onEdit }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();

    function paymentLabel(booking) {
        const method = booking.paymentMethod ? labels.enum(booking.paymentMethod) : format.none;
        if (!booking.paymentStatus) return method;
        return t("booking.table.payment", { method, status: labels.enum(booking.paymentStatus) });
    }

    const columns = [
        { key: "reference", header: t("booking.table.ref"), className: "whitespace-nowrap" },
        { key: "activityName", header: t("booking.table.activity") },
        {
            key: "slot",
            header: t("booking.table.dateSlot"),
            className: "whitespace-nowrap",
            cell: (booking) =>
                t("booking.table.slot", {
                    date: format.dateShort(booking.slotDate),
                    time: format.timeRange(booking.slotStartTime, booking.slotEndTime),
                }),
        },
        { key: "peopleCount", header: t("booking.table.people") },
        { key: "payment", header: t("booking.table.paymentHeader"), className: "whitespace-nowrap", cell: paymentLabel },
        { key: "status", header: t("booking.table.status"), cell: (booking) => <StatusTag value={booking.status} /> },
    ];

    return (
        <Table
            caption={t("booking.bookingsTitle")}
            columns={columns}
            rows={bookings}
            emptyMessage={t("booking.table.empty")}
            rowActions={(booking) => {
                const isPending = pendingAction?.id === booking.id;
                const failed = actionError?.id === booking.id;
                return (
                    <div className="flex flex-col gap-1">
                        <div className="flex flex-wrap items-center gap-1">
                            {rowActions(booking).map((action) => (
                                <IconButton
                                    key={action}
                                    icon={ACTION_ICONS[action]}
                                    label={t(`booking.actions.${action}`)}
                                    tone={action === "cancel" ? "danger" : "primary"}
                                    disabled={isPending}
                                    onClick={() => (action === "edit" ? onEdit(booking) : onAction(booking, action))}
                                />
                            ))}
                        </div>
                        {failed && <p className="max-w-xs text-xs text-error-fg">{actionError.message}</p>}
                    </div>
                );
            }}
        />
    );
}
