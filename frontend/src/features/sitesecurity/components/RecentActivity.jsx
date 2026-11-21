import { useTranslation } from "react-i18next";
import { ArrowUpRight } from "lucide-react";
import { Button, Card, EmptyState, ErrorState, Skeleton } from "../../../shared/ui";
import { StatusLabel } from "./ui";
import { useAccessLogs } from "../hooks/useAccessLogs";
import { recentEvents } from "../utils/accessLogs";

/* Rendered only for callers allowed to read the access log. */
export default function RecentActivity({ onOpenLog }) {
  const { t } = useTranslation("sitesecurity");
  const { allLogs, isLoading, error } = useAccessLogs();
  const feed = recentEvents(allLogs);

  const sentence = (event) => {
    const user = event.user ?? t("log.unidentified");
    if (!event.personId) return t("dashboard.activity.attempted", { user, zone: event.zoneName });
    return event.action === "Entry"
      ? t("dashboard.activity.entered", { user, zone: event.zoneName })
      : t("dashboard.activity.exited", { user, zone: event.zoneName });
  };

  return (
    <Card
      title={t("dashboard.activity.title")}
      actions={
        <Button variant="secondary" size="sm" icon={ArrowUpRight} onClick={() => onOpenLog()}>
          {t("dashboard.activity.viewAll")}
        </Button>
      }
    >
      {isLoading && <Skeleton className="h-24 w-full" />}
      {!isLoading && error && <ErrorState message={t("log.loadError")} />}
      {!isLoading && !error && feed.length === 0 && <EmptyState message={t("dashboard.activity.empty")} />}
      {feed.length > 0 && (
        <ul className="divide-y divide-border-subtle">
          {feed.map((event) => (
            <li key={event.id} className="flex items-center gap-4 py-3">
              <p className="flex-1 text-sm text-text">{sentence(event)}</p>
              <StatusLabel status={event.status} />
              <span className="w-16 text-right text-xs tabular-nums text-text-muted">{event.time}</span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
