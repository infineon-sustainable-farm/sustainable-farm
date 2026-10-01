import { UserPlus } from "lucide-react";
import { Loader2, TriangleAlert, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useEventRegistrations } from "../hooks/useEvents";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";
import StatusBadge from "./StatusBadge";

const COLUMNS = ["Visitor", "Group", "Purpose", "Status"];

/**
 * Participants panel of one event, opened from a card. It is read-only: the
 * attendee registration form lives on the Registration screen (target:
 * Event), which is the single entry point for registering visitors.
 */
export default function EventParticipants({ event, onClose }) {
    const navigate = useNavigate();
    const {
        data: registrations,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useEventRegistrations(event.id);

    function goToRegistration() {
        navigate(`/visitormanagement/registration?eventId=${event.id}`);
    }

    return (
        <section className="mb-6 rounded-lg border border-line bg-[#F7FDFB] p-4.5 animate-fade-up">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <h3 className="font-heading text-[17px] font-bold text-ink">
                        Participants — {event.title}
                    </h3>
                    <p className="mt-0.5 text-[11px] text-muted">
                        {event.booked > 0 ? `${event.booked} registered` : "No registration yet"}
                        {` · max ${event.maxCapacity}`}
                    </p>
                </div>
                <div className="flex shrink-0 items-center gap-1.5">
                    <button
                        type="button"
                        onClick={goToRegistration}
                        title="Register a visitor on this event"
                        className="flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        <UserPlus size={14} />
                        Register visitor
                    </button>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close participants"
                        className="rounded-md border border-line bg-white p-1.5 text-primary hover:bg-[#F2FBF9]"
                    >
                        <X size={16} />
                    </button>
                </div>
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 px-6 py-10 text-sm text-muted">
                    <Loader2 size={18} className="animate-spin" />
                    Loading participants…
                </div>
            )}

            {isError && (
                <div className="mt-4 flex flex-col items-center gap-3 rounded-lg border border-error/30 bg-error/5 px-6 py-8 text-center">
                    <TriangleAlert size={24} className="text-error" />
                    <p className="text-sm text-muted">Participants could not be loaded.</p>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && (
                <div className="mt-3 overflow-x-auto">
                    <table className="vm-table w-full border-collapse text-sm">
                        <thead>
                            <tr>
                                {COLUMNS.map((column) => (
                                    <th
                                        key={column}
                                        scope="col"
                                        className="border-b-2 border-line bg-white px-3 py-2 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                                    >
                                        {column}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {(registrations ?? []).length === 0 && (
                                <tr>
                                    <td
                                        colSpan={COLUMNS.length}
                                        className="px-3 py-6 text-center text-muted"
                                    >
                                        No participant yet.
                                    </td>
                                </tr>
                            )}
                            {(registrations ?? []).map((registration) => (
                                <tr key={registration.id} className="border-b border-line">
                                    <td className="px-3 py-2.5 font-medium text-ink">
                                        {registration.visitorName ?? "—"}
                                    </td>
                                    <td className="px-3 py-2.5 text-ink">
                                        {registration.groupSize ?? "—"}
                                    </td>
                                    <td className="px-3 py-2.5 text-ink">
                                        {formatEnumLabel(registration.visitPurpose)}
                                    </td>
                                    <td className="px-3 py-2.5">
                                        <StatusBadge value={registration.status} />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <p className="mt-4 rounded-md border border-line bg-white px-3 py-2 text-xs text-muted">
                “Register visitor” opens the Registration screen with this event preselected; the
                visitor is then registered on the event.
            </p>
        </section>
    );
}