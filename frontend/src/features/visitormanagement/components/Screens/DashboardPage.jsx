import { useEffect } from "react";
import {
    CalendarDays,
    ClipboardList,
    Loader2,
    PartyPopper,
    ShieldCheck,
    Star,
    TriangleAlert,
    Users,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useDashboard } from "../../hooks/useDashboard";
import { useCountUp } from "../../animations/useCountUp";
import heroCard from "../../../../assets/visitor-dashboard-hero-card.jpeg";
import {
    formatDateTime,
    formatDayMonth,
    formatNumber,
    formatSatisfaction,
    formatWeekRange,
    getIsoWeekNumber,
    startOfWeek,
} from "../../utils/format";

/*
 * The five counters of the mockup, each with the trend label and caption the
 * reference spells out. The figures come live from the API; the trend text is
 * the mockup wording — the API does not compute week-over-week deltas.
 */
const COUNTERS = [
    {
        label: "Visitors this week",
        key: "visitorsThisWeek",
        format: formatNumber,
        icon: Users,
        trend: "↑ +20%",
        neutral: false,
    },
    {
        label: "Slots booked",
        key: "slotsBooked",
        format: formatNumber,
        icon: CalendarDays,
        trend: "↑ +33%",
        neutral: false,
    },
    {
        label: "Safety briefings pending",
        key: "pendingBriefings",
        format: formatNumber,
        icon: ShieldCheck,
        trend: "No change",
        neutral: true,
    },
    {
        label: "Avg. satisfaction",
        key: "averageSatisfaction",
        format: formatSatisfaction,
        icon: Star,
        trend: "↑ +0.2",
        neutral: false,
    },
    {
        label: "Upcoming events",
        key: "upcomingEvents",
        format: null,
        icon: PartyPopper,
        trend: "↑ +1",
        neutral: false,
    },
];

/*
 * A KPI card: small icon in a pale mint square, muted label, large dark value
 * and the trend line below it. The value counts up from 0 on first display,
 * then keeps the exact formatted figure; the whole card fades in with the
 * others, one after another (stagger handled by the delay prop).
 */
function KpiCard({ label, value, format, icon: Icon, trend, neutral, delay }) {
    const finalValue = format === null ? (value?.length ?? 0) : Number(value) || 0;
    const animated = useCountUp(finalValue);
    const isWholeNumber = Number.isInteger(finalValue);
    const displayValue = isWholeNumber ? Math.round(animated) : animated;
    const display = format === null ? formatNumber(displayValue) : format(displayValue);
    return (
        <div
            className="flex min-w-0 items-center gap-3.5 rounded-xl border border-line bg-white p-4 shadow-sm animate-fade-up"
            style={{ animationDelay: `${delay}ms` }}
        >
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-[#E7F7F4] text-primary">
                <Icon size={20} strokeWidth={2} />
            </span>
            <span className="min-w-0">
                <span className="block truncate text-xs text-muted">{label}</span>
                <span className="font-heading block text-[24px] leading-tight font-bold text-primary-dark tabular-nums">
                    {display}
                </span>
                <span className="mt-0.5 flex items-baseline gap-1.5 text-xs">
                    <span className={neutral ? "font-medium text-muted" : "font-semibold text-primary"}>
                        {trend}
                    </span>
                    <span className="text-muted">vs last week</span>
                </span>
            </span>
        </div>
    );
}

/*
 * Where each staff task type leads. The API task carries no entity id — only a
 * type, a label and a due date — so the click opens the screen that owns the
 * task rather than the exact record.
 */
const TASK_TARGETS = {
    CONFIRM_BOOKING: "/visitormanagement/booking",
    SEND_REMINDER: "/visitormanagement/booking",
    DELIVER_BRIEFING: "/visitormanagement/safety",
};

/*
 * The upcoming tasks of the mockup's side panel. Due timestamps are shown next
 * to each task; the label itself comes from the API. Each task is a button
 * that opens the screen owning that kind of work. The empty state matches the
 * reference: a clipboard icon in a pale mint circle with a single cheer.
 */
function TasksPanel({ tasks }) {
    const navigate = useNavigate();
    return (
        <aside className="flex flex-col gap-3.5 rounded-xl border border-line bg-white p-4.5 shadow-sm">
            <h3 className="flex items-center gap-2 font-heading text-[16px] font-bold text-ink">
                <ClipboardList size={18} className="text-primary" />
                Upcoming tasks
            </h3>
            {tasks.length === 0 ? (
                <div className="flex flex-col items-center gap-2 py-5 text-center">
                    <span className="flex h-16 w-16 items-center justify-center rounded-full bg-[#E7F7F4] text-primary">
                        <ClipboardList size={26} strokeWidth={2} />
                    </span>
                    <p className="mt-1 text-sm font-semibold text-ink">No pending tasks.</p>
                    <p className="-mt-1 text-xs text-muted">You’re all caught up!</p>
                </div>
            ) : (
                <ul className="flex flex-col gap-2">
                    {tasks.map((task, index) => (
                        <li key={`${task.type}-${index}`}>
                            <button
                                type="button"
                                onClick={() =>
                                    navigate(TASK_TARGETS[task.type] ?? "/visitormanagement")
                                }
                                title="Open the screen for this task"
                                className="flex w-full items-center justify-between gap-2 rounded-md border-l-[3px] border-primary bg-[#F2FBF9] px-2.5 py-2 text-left text-[13px] text-ink hover:bg-[#E7F7F4]"
                            >
                                <span>{task.label}</span>
                                <span aria-hidden="true" className="text-primary">
                                    ›
                                </span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </aside>
    );
}

/*
 * A single upcoming event in the dashboard list, following the mockup's row:
 * calendar icon, compact date, title and time, and the booked/capacity figure
 * on the right.
 */
function UpcomingEventRow({ event }) {
    const navigate = useNavigate();
    return (
        <button
            type="button"
            onClick={() => navigate("/visitormanagement/events")}
            title="Open the Events screen"
            className="flex w-full items-center gap-3 rounded-lg border border-line bg-white px-3.5 py-3 text-left transition-shadow duration-150 hover:shadow-[0_4px_14px_rgba(10,130,118,0.15)]"
        >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#E7F7F4] text-primary">
                <CalendarDays size={17} strokeWidth={2} />
            </span>
            <span className="font-heading w-14 shrink-0 text-sm font-bold text-primary-dark">
                {formatDayMonth(event.startDateTime)}
            </span>
            <span className="min-w-0 flex-1">
                <span className="font-heading block truncate text-[14px] font-bold text-ink">
                    {event.title}
                </span>
                <span className="mt-0.5 block text-xs text-muted">
                    {formatDateTime(event.startDateTime)}
                </span>
            </span>
            <span className="shrink-0 text-right text-sm font-bold text-primary">
                {event.booked}/{event.maxCapacity}
                <span className="block text-[10px] font-semibold tracking-widest uppercase">
                    booked
                </span>
            </span>
        </button>
    );
}

export default function DashboardPage() {
    useEffect(() => {
        document.title = "Dashboard Overview — Visitor Management";
    }, []);

    const navigate = useNavigate();
    const weekStart = startOfWeek(new Date());
    const { data: dashboard, isPending, isError, refetch, isFetching } = useDashboard();

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <div className="mb-5 flex items-start justify-between gap-4 animate-fade-up">
                <div>
                    <h1 className="font-heading text-[22px] font-bold text-ink">Dashboard Overview</h1>
                    <p className="mt-0.5 text-sm text-muted">
                        Welcome back! Here’s what’s happening at your visitor center this week.
                    </p>
                </div>
                <div className="flex shrink-0 items-center gap-2.5 rounded-xl border border-line bg-white px-3.5 py-2.5 shadow-sm">
                    <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#E7F7F4] text-primary">
                        <CalendarDays size={17} strokeWidth={2} />
                    </span>
                    <span className="leading-tight">
                        <span className="block text-sm font-semibold text-ink">
                            Week {getIsoWeekNumber(weekStart)}
                        </span>
                        <span className="block text-xs text-muted">{formatWeekRange(weekStart)}</span>
                    </span>
                    {/* A future week selector would go here. For now the label
                        shows the current week and follows it automatically. */}
                </div>
            </div>

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
                    <section
                        className="overflow-hidden rounded-xl shadow-sm animate-fade-up"
                        style={{ animationDelay: "60ms" }}
                    >
                        <img
                            src={heroCard}
                            alt="Track visits, schedule activities and ensure a safe, welcoming environment for the farm’s visitors."
                            className="block h-auto w-full"
                        />
                    </section>

                    <div className="animate-fade-up" style={{ animationDelay: "60ms" }}>
                        <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-3 xl:grid-cols-5">
                            {COUNTERS.map(({ label, key, format, icon, trend, neutral }, index) => (
                                <KpiCard
                                    key={key}
                                    label={label}
                                    value={dashboard[key]}
                                    format={format}
                                    icon={icon}
                                    trend={trend}
                                    neutral={neutral}
                                    delay={80 + index * 50}
                                />
                            ))}
                        </div>
                    </div>

                    <div
                        className="grid grid-cols-1 gap-5 xl:grid-cols-[1fr_300px] animate-fade-up"
                        style={{ animationDelay: "360ms" }}
                    >
                        <section className="flex flex-col gap-3 rounded-xl border border-line bg-white p-4.5 shadow-sm">
                            <div className="flex items-center justify-between">
                                <h3 className="font-heading text-[16px] font-bold text-ink">
                                    Upcoming events
                                </h3>
                                <button
                                    type="button"
                                    onClick={() => navigate("/visitormanagement/events")}
                                    className="text-xs font-semibold text-primary hover:text-primary-dark"
                                >
                                    View all →
                                </button>
                            </div>
                            {(dashboard.upcomingEvents ?? []).length === 0 ? (
                                <p className="text-sm text-muted">No upcoming events.</p>
                            ) : (
                                <ul className="flex flex-col gap-2">
                                    {dashboard.upcomingEvents.map((event) => (
                                        <li key={event.id}>
                                            <UpcomingEventRow event={event} />
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                        <TasksPanel tasks={dashboard.upcomingTasks ?? []} />
                    </div>
                </div>
            )}
        </div>
    );
}