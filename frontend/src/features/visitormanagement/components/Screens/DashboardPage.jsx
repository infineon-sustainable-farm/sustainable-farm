import { useEffect } from "react";
import {
    ArrowRight,
    CalendarDays,
    ChevronRight,
    ClipboardList,
    PartyPopper,
    ShieldCheck,
    Star,
    Users,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useDashboard } from "../../hooks/useDashboard";
import heroCard from "../../../../assets/visitor-dashboard-hero-card.jpeg";
import { getIsoWeekNumber, startOfWeek, useVisitorFormat } from "../../utils/format";
import { Button, Card, EmptyState, KpiCard, PageHeader, Skeleton } from "../../../../shared/ui";
import { LoadError } from "../LoadState";

/*
 * The five counters of the dashboard. The figures come live from the API and show their final
 * value at once.
 */
const COUNTERS = [
    { key: "visitorsThisWeek", labelKey: "dashboard.visitorsThisWeek", icon: Users, kind: "number" },
    { key: "slotsBooked", labelKey: "dashboard.slotsBooked", icon: CalendarDays, kind: "number" },
    { key: "pendingBriefings", labelKey: "dashboard.pendingBriefings", icon: ShieldCheck, kind: "number" },
    { key: "averageSatisfaction", labelKey: "dashboard.averageSatisfaction", icon: Star, kind: "satisfaction" },
    { key: "upcomingEvents", labelKey: "dashboard.upcomingEvents", icon: PartyPopper, kind: "count" },
];

/*
 * Where each staff task type leads. The API task carries no entity id, only a type, a label and a
 * due date, so the click opens the screen that owns the task rather than the exact record.
 */
const TASK_TARGETS = {
    CONFIRM_BOOKING: "/visitormanagement/booking",
    SEND_REMINDER: "/visitormanagement/booking",
    DELIVER_BRIEFING: "/visitormanagement/safety",
};

function counterValue(counter, dashboard, format) {
    const value = dashboard[counter.key];
    if (counter.kind === "count") return format.number(value?.length ?? 0);
    if (counter.kind === "satisfaction") return format.satisfaction(Number(value) || 0);
    return format.number(Number(value) || 0);
}

/** The upcoming tasks; each one opens the screen that owns that kind of work. */
function TasksPanel({ tasks }) {
    const { t } = useTranslation("visitormanagement");
    const navigate = useNavigate();
    return (
        <Card
            as="aside"
            title={
                <span className="flex items-center gap-2">
                    <ClipboardList size={18} aria-hidden="true" className="text-primary" />
                    {t("dashboard.upcomingTasks")}
                </span>
            }
        >
            {tasks.length === 0 ? (
                <EmptyState
                    className="py-6"
                    icon={ClipboardList}
                    title={t("dashboard.noTasks")}
                    message={t("dashboard.allCaughtUp")}
                />
            ) : (
                <ul className="flex flex-col gap-2">
                    {tasks.map((task, index) => (
                        <li key={`${task.type}-${index}`}>
                            <button
                                type="button"
                                onClick={() => navigate(TASK_TARGETS[task.type] ?? "/visitormanagement")}
                                title={t("dashboard.openTask")}
                                className="flex w-full items-center justify-between gap-2 rounded-control border-l-4 border-primary bg-primary-tint px-3 py-2 text-left text-sm text-text transition duration-(--duration-fast) ease-standard hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring motion-reduce:transition-none"
                            >
                                <span>{task.label}</span>
                                <ChevronRight size={16} aria-hidden="true" className="shrink-0 text-primary" />
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </Card>
    );
}

/** A single upcoming event: date, title and time, and the booked over capacity figure. */
function UpcomingEventRow({ event }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const navigate = useNavigate();
    return (
        <button
            type="button"
            onClick={() => navigate("/visitormanagement/events")}
            title={t("dashboard.openEvents")}
            className="flex w-full items-center gap-3 rounded-tile border border-border-subtle bg-surface-raised px-4 py-3 text-left transition duration-(--duration-fast) ease-standard hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring motion-reduce:transition-none"
        >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-tile bg-primary-tint text-primary">
                <CalendarDays size={18} aria-hidden="true" />
            </span>
            <span className="w-14 shrink-0 text-sm font-bold text-text">{format.dayMonth(event.startDateTime)}</span>
            <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-text">{event.title}</span>
                <span className="mt-0.5 block text-xs text-text-muted">{format.dateTime(event.startDateTime)}</span>
            </span>
            <span className="shrink-0 text-right text-sm font-bold text-text-accent">
                {t("dashboard.bookedRatio", { booked: event.booked, capacity: event.maxCapacity })}
                <span className="block text-xs font-semibold uppercase">{t("dashboard.booked")}</span>
            </span>
        </button>
    );
}

function DashboardSkeleton() {
    const { t } = useTranslation("visitormanagement");
    return (
        <div className="flex flex-col gap-5" aria-busy="true">
            <p role="status" className="sr-only">
                {t("dashboard.loading")}
            </p>
            <Skeleton radius="card" className="h-44" />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
                {[0, 1, 2, 3, 4].map((index) => (
                    <Skeleton key={index} radius="card" className="h-40" />
                ))}
            </div>
            <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                <Skeleton radius="card" className="h-64 xl:col-span-2" />
                <Skeleton radius="card" className="h-64" />
            </div>
        </div>
    );
}

export default function DashboardPage() {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const navigate = useNavigate();

    useEffect(() => {
        document.title = t("dashboard.documentTitle");
    }, [t]);

    const weekStart = startOfWeek(new Date());
    const { data: dashboard, isPending, isError, refetch, isFetching } = useDashboard();

    return (
        <div className="space-y-6 p-6">
            <PageHeader
                title={t("dashboard.title")}
                subtitle={t("dashboard.subtitle")}
                meta={
                    <span className="flex items-center gap-2">
                        <CalendarDays size={16} aria-hidden="true" className="text-primary" />
                        <span className="font-semibold text-text">
                            {t("dashboard.week", { week: getIsoWeekNumber(weekStart) })}
                        </span>
                        <span>{format.weekRange(weekStart)}</span>
                    </span>
                }
            />

            {isPending && <DashboardSkeleton />}

            {isError && (
                <LoadError title={t("dashboard.loadError")} onRetry={() => refetch()} retrying={isFetching} />
            )}

            {!isPending && !isError && dashboard && (
                <>
                    <section className="overflow-hidden rounded-card border border-border-subtle shadow-raised">
                        <img src={heroCard} alt={t("dashboard.heroAlt")} className="block h-auto w-full" />
                    </section>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
                        {COUNTERS.map((counter) => (
                            <KpiCard
                                key={counter.key}
                                icon={counter.icon}
                                label={t(counter.labelKey)}
                                value={counterValue(counter, dashboard, format)}
                            />
                        ))}
                    </div>

                    <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                        <Card
                            className="xl:col-span-2"
                            title={t("dashboard.upcomingEvents")}
                            actions={
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => navigate("/visitormanagement/events")}
                                >
                                    {t("dashboard.viewAll")}
                                    <ArrowRight size={16} aria-hidden="true" />
                                </Button>
                            }
                        >
                            {(dashboard.upcomingEvents ?? []).length === 0 ? (
                                <p className="text-sm text-text-muted">{t("dashboard.noEvents")}</p>
                            ) : (
                                <ul className="flex flex-col gap-2">
                                    {dashboard.upcomingEvents.map((event) => (
                                        <li key={event.id}>
                                            <UpcomingEventRow event={event} />
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </Card>
                        <TasksPanel tasks={dashboard.upcomingTasks ?? []} />
                    </div>
                </>
            )}
        </div>
    );
}
