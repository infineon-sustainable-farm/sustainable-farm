import StatusBadge from "./StatusBadge";
import { formatDateShort, formatNumber, formatTimeRange } from "../utils/format";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

/*
 * Publish is only legal from DRAFT; cancel is legal until the event is
 * COMPLETED. There is no manual "complete" call — the visit lifecycle job
 * completes past events — so the mockup's complete button has no counterpart.
 * The participants action opens the attendee panel for any status.
 */
function cardActions(status) {
    if (status === "DRAFT") return ["participants", "publish", "edit", "cancel"];
    if (status === "PUBLISHED") return ["participants", "edit", "cancel"];
    return ["participants"];
}

export default function EventCard({
    event,
    pendingAction,
    actionError,
    onAction,
    onEdit,
    onParticipants,
    delay = 0,
}) {
    const isPending = pendingAction?.id === event.id;
    const failed = actionError?.id === event.id;

    const date = event.startDateTime?.slice(0, 10);
    const startTime = event.startDateTime?.slice(11, 19);
    const endTime = event.endDateTime?.slice(11, 19);

    return (
        <article
            className="flex min-h-[150px] flex-col rounded-lg border border-line bg-white p-4.5 animate-fade-up"
            style={{ animationDelay: `${delay}ms` }}
        >
            <h3 className="font-heading text-[17px] leading-snug font-bold text-ink">
                {event.title}
            </h3>
            <p className="mt-2 text-[11px] text-muted">
                {formatDateShort(date)}
                {" · "}
                {formatTimeRange(startTime, endTime)}
                {event.location ? ` · ${event.location}` : ""}
                {` · Max ${formatNumber(event.maxCapacity)}`}
            </p>
            <p className="mt-1 text-[11px] text-muted">
                {event.booked > 0
                    ? `${formatNumber(event.booked)} registered`
                    : "No registration yet"}
            </p>

            <div className="mt-2.5">
                <StatusBadge value={event.status} />
            </div>

            <div className="mt-auto flex flex-wrap items-center gap-1.5 pt-3">
                {cardActions(event.status).map((action) => (
                    <button
                        key={action}
                        type="button"
                        disabled={isPending}
                        onClick={() => {
                            if (action === "edit") onEdit(event);
                            else if (action === "participants") onParticipants(event);
                            else onAction(event, action);
                        }}
                        className={action === "cancel" ? TINY_DANGER : TINY_BUTTON}
                    >
                        {action === "participants" && "participants"}
                        {action === "publish" && "✓ publish"}
                        {action === "edit" && "✎ edit"}
                        {action === "cancel" && "✕ cancel"}
                    </button>
                ))}
                {failed && (
                    <p className="w-full text-[11px] text-error">{actionError.message}</p>
                )}
            </div>
        </article>
    );
}