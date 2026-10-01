import { useState } from "react";
import { formatDateTime } from "../utils/format";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";

const COLUMNS = ["Visitor", "Date", "Rating", "Comment", "Routed to"];

/**
 * The "Routed to" cell: the mockup only displays the team, so the routing
 * action lives in the same cell — a "route" button reveals a small input when
 * the response has not been routed yet. The API caps the team name at 60
 * characters.
 */
function RouteCell({ feedback, isRouting, routeError, onRoute }) {
    const [editing, setEditing] = useState(false);
    const [routedTo, setRoutedTo] = useState("");
    const [fieldError, setFieldError] = useState(null);

    if (feedback.routedTo) {
        return <span className="text-ink">{feedback.routedTo}</span>;
    }

    if (!editing) {
        return (
            <button type="button" disabled={isRouting} onClick={() => setEditing(true)} className={TINY_BUTTON}>
                route
            </button>
        );
    }

    function handleSubmit(event) {
        event.preventDefault();
        const value = routedTo.trim();
        if (!value) {
            setFieldError("Team is required.");
            return;
        }
        if (value.length > 60) {
            setFieldError("Team must be at most 60 characters.");
            return;
        }
        setFieldError(null);
        onRoute(feedback.id, { routedTo: value });
        setEditing(false);
    }

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-1">
            <div className="flex items-center gap-1.5">
                <input
                    type="text"
                    value={routedTo}
                    onChange={(event) => setRoutedTo(event.target.value)}
                    placeholder="e.g. Sales"
                    className="w-28 rounded-md border border-line bg-[#F7FDFB] px-2 py-1 text-xs"
                />
                <button type="submit" disabled={isRouting} className={TINY_BUTTON}>
                    save
                </button>
                <button
                    type="button"
                    disabled={isRouting}
                    onClick={() => {
                        setEditing(false);
                        setFieldError(null);
                    }}
                    className={TINY_BUTTON}
                >
                    cancel
                </button>
            </div>
            {(fieldError ?? routeError) && (
                <span className="text-[11px] text-error">{fieldError ?? routeError}</span>
            )}
        </form>
    );
}

export default function FeedbackTable({ feedback, routingId, routeError, onRoute }) {
    return (
        <div className="overflow-x-auto">
            <table className="vm-table w-full border-collapse text-sm">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b-2 border-line bg-[#F4F8F8] px-3 py-2.5 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {feedback.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No feedback received yet.
                            </td>
                        </tr>
                    )}
                    {feedback.map((response) => (
                        <tr key={response.id} className="border-b border-line align-top">
                            <td className="px-3 py-2.5 font-medium text-ink">
                                {response.visitorName ?? "—"}
                            </td>
                            <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                {formatDateTime(response.submittedAt)}
                            </td>
                            <td className="px-3 py-2.5 text-ink">{response.rating}★</td>
                            <td className="max-w-[320px] px-3 py-2.5 text-ink">
                                {response.comment ?? "—"}
                            </td>
                            <td className="px-3 py-2.5">
                                <RouteCell
                                    feedback={response}
                                    isRouting={routingId === response.id}
                                    routeError={routeError?.id === response.id ? routeError.message : null}
                                    onRoute={onRoute}
                                />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}