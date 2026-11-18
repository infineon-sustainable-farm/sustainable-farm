import { CircleX, Pencil, Send, Users } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card } from "../../../shared/ui";
import { useVisitorFormat } from "../utils/format";
import StatusTag from "./StatusTag";

/*
 * Publish is only legal from DRAFT; cancel is legal until the event is COMPLETED. There is no
 * manual "complete" call (the visit lifecycle job completes past events). The participants
 * action opens the attendee panel for any status.
 */
function cardActions(status) {
    if (status === "DRAFT") return ["participants", "publish", "edit", "cancel"];
    if (status === "PUBLISHED") return ["participants", "edit", "cancel"];
    return ["participants"];
}

const ACTION_ICONS = { participants: Users, publish: Send, edit: Pencil, cancel: CircleX };

export default function EventCard({ event, pendingAction, actionError, onAction, onEdit, onParticipants }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const isPending = pendingAction?.id === event.id;
    const failed = actionError?.id === event.id;

    const date = event.startDateTime?.slice(0, 10);
    const startTime = event.startDateTime?.slice(11, 19);
    const endTime = event.endDateTime?.slice(11, 19);

    const meta = [format.dateShort(date), format.timeRange(startTime, endTime)];
    if (event.location) meta.push(event.location);
    meta.push(t("events.card.max", { value: format.number(event.maxCapacity) }));

    return (
        <Card as="article" title={event.title} titleAs="h3" className="flex flex-col">
            <p className="-mt-2 text-xs text-text-muted">{meta.join(" · ")}</p>
            <p className="mt-1 text-xs text-text-muted">
                {event.booked > 0
                    ? t("events.registered", { value: format.number(event.booked) })
                    : t("events.noRegistration")}
            </p>

            <div className="mt-3">
                <StatusTag value={event.status} />
            </div>

            <div className="mt-auto flex flex-wrap items-center gap-2 pt-4">
                {cardActions(event.status).map((action) => (
                    <Button
                        key={action}
                        size="sm"
                        variant="secondary"
                        icon={ACTION_ICONS[action]}
                        disabled={isPending}
                        onClick={() => {
                            if (action === "edit") onEdit(event);
                            else if (action === "participants") onParticipants(event);
                            else onAction(event, action);
                        }}
                    >
                        {t(`events.card.actions.${action}`)}
                    </Button>
                ))}
                {failed && (
                    <p role="alert" className="w-full text-xs text-error-fg">
                        {actionError.message}
                    </p>
                )}
            </div>
        </Card>
    );
}
