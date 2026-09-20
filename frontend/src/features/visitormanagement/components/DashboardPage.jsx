import { useEffect } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useDashboard } from "../hooks/useDashboard";
import { formatNumber, formatSatisfaction, getIsoWeekNumber } from "../utils/format";

/*
 * The five counter cards of the mockup, in its order and with its wording.
 * "Slots booked" is the count of booked slots; the backend does not expose the
 * week's total, so the mockup's "6/12" fraction is not reproduced.
 */
const COUNTERS = [
    { label: "Visitors this week", key: "visitorsThisWeek", format: formatNumber },
    { label: "Slots booked", key: "slotsBooked", format: formatNumber },
    { label: "Safety briefings pending", key: "pendingBriefings", format: formatNumber },
    { label: "Avg. satisfaction", key: "averageSatisfaction", format: formatSatisfaction },
    { label: "Upcoming events", key: "upcomingEvents", format: null },
];

/*
 * The mockup's quick-access grid. Each card opens the screen listed here; the
 * path field is only present once that screen actually exists (the sidebar
 * follows the same rule). Planned cards keep the mockup look but stay inert.
 */
const FEATURE_CARDS = [
    { title: "Farm tour scheduling", target: "scheduling", path: "/visitormanagement/scheduling" },
    { title: "Visitor registration", target: "registration", path: "/visitormanagement/registration" },
    { title: "Educational program", target: "education", path: "/visitormanagement/education" },
    { title: "Safety briefings tracking", target: "safety", path: "/visitormanagement/safety" },
    { title: "Agritourism booking system", target: "booking", path: "/visitormanagement/booking" },
    { title: "Visitor feedback collection", target: "feedback", path: "/visitormanagement/feedback" },
    { title: "Events", target: "events", path: "/visitormanagement/events" },
];

function CounterCard({ label, value, format }) {
    const display = format === null ? formatNumber(value?.length) : format(value);
    return (
        <div className="rounded-lg border border-line bg-white px-4 py-4">
            <p className="mb-1.5 text-xs text-muted">{label}</p>
            <p className="font-heading text-[28px] leading-none font-bold text-primary-dark">
                {display}
            </p>
        </div>
    );
}

/*
 * A quick-access card as a button only when its screen exists; otherwise a
 * disabled span with the same look. Both states carry the exact mockup wording,
 * including "Open →".
 */
function FeatureCard({ card, built }) {
    const navigate = useNavigate();
    const body = (
        <>
            <span className="font-heading text-[17px] leading-snug font-bold text-ink">
                {card.title}
            </span>
            <span className="mt-2.5 text-xs text-primary">Open →</span>
        </>
    );
    const classes =
        "flex min-h-[110px] flex-col justify-between rounded-lg border border-line bg-white p-4.5";
    const inner = built ? (
        <button
            type="button"
            onClick={() => navigate(card.path)}
            className={`${classes} cursor-pointer text-left transition-shadow duration-150 hover:shadow-[0_4px_14px_rgba(10,130,118,0.15)]`}
        >
            {body}
        </button>
    ) : (
        <span aria-disabled="true" className={`${classes} cursor-not-allowed`}>
            {body}
            <span className="sr-only"> (planned, not available yet)</span>
        </span>
    );
    return inner;
}

/*
 * The upcoming tasks of the mockup's side panel. Due timestamps are shown next
 * to each task; the label itself comes from the API.
 */
function TasksPanel({ tasks }) {
    return (
        <aside className="flex flex-col gap-3.5 rounded-lg border border-line bg-white p-4.5">
            <h3 className="font-heading text-[17px] font-bold text-ink">Upcoming tasks</h3>
            {tasks.length === 0 ? (
                <p className="text-sm text-muted">No pending tasks.</p>
            ) : (
                <ul className="flex flex-col gap-2">
                    {tasks.map((task, index) => (
                        <li
                            key={`${task.type}-${index}`}
                            className="rounded-md border-l-[3px] border-primary bg-[#F2FBF9] px-2.5 py-2 text-[13px] text-ink"
                        >
                            {task.label}
                        </li>
                    ))}
                </ul>
            )}
            <p className="font-heading mx-auto flex h-[110px] w-[110px] items-center justify-center rounded-full border-[3px] border-primary text-center text-sm leading-tight font-bold text-primary-dark">
                Week {getIsoWeekNumber(new Date())}
                <br />
                Overview
            </p>
        </aside>
    );
}

export default function DashboardPage() {
    useEffect(() => {
        document.title = "Dashboard Overview — Visitor Management";
    }, []);

    const { data: dashboard, isPending, isError, refetch, isFetching } = useDashboard();

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                    <Loader2 size={18} className="animate-spin" />
                    Loading dashboard…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-ink">
                            Dashboard could not be loaded
                        </p>
                        <p className="mt-1 text-sm text-muted">
                            The request to the server did not succeed.
                        </p>
                    </div>
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

            {!isPending && !isError && dashboard && (
                <div className="flex flex-col gap-5">
                    <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-5">
                        {COUNTERS.map(({ label, key, format }) => (
                            <CounterCard
                                key={key}
                                label={label}
                                value={dashboard[key]}
                                format={format}
                            />
                        ))}
                    </div>

                    <div className="grid grid-cols-1 gap-5 xl:grid-cols-[1fr_280px]">
                        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 xl:grid-cols-3">
                            {FEATURE_CARDS.map((card) => (
                                <FeatureCard key={card.target} card={card} built={Boolean(card.path)} />
                            ))}
                        </div>
                        <TasksPanel tasks={dashboard.upcomingTasks ?? []} />
                    </div>
                </div>
            )}
        </div>
    );
}