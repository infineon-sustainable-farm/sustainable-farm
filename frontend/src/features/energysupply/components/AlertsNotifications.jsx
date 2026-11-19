import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getAllAlerts, updateAlertStatus } from "../api/energyApi";
import { severityTone, useEnergyFormat, useEnumLabels } from "../format";
import { Button, Card, ErrorState, PageHeader, StatusBadge, Table, Tabs } from "../../../shared/ui";
import InlineAlert from "./InlineAlert";

const FILTERS = ["All", "Open", "Resolved"];
const isStatus = (alert, status) => alert.status === status.toUpperCase();

export default function AlertsNotifications() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [alerts, setAlerts] = useState(null);
  const [filter, setFilter] = useState("All");
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  const refresh = useCallback(async () => {
    try {
      setAlerts(await getAllAlerts());
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    getAllAlerts().then(setAlerts).catch((err) => setError(err.message));
  }, []);

  async function resolve(alert) {
    setUpdatingId(alert.alertId);
    setError(null);
    try {
      await updateAlertStatus(alert.alertId, "RESOLVED");
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setUpdatingId(null);
    }
  }

  const rows = useMemo(
    () => (!alerts ? [] : filter === "All" ? alerts : alerts.filter((a) => isStatus(a, filter))),
    [alerts, filter]
  );

  if (error && !alerts) {
    return (
      <div className="p-6">
        <ErrorState message={t("alerts.loadError", { error })} />
      </div>
    );
  }
  if (!alerts) return <p role="status" className="p-6 text-sm text-text-muted">{t("alerts.loading")}</p>;

  const openCount = alerts.filter((a) => isStatus(a, "Open")).length;
  const criticalCount = alerts.filter((a) => a.severity === "CRITICAL").length;

  const columns = [
    { key: "alertId", header: t("alerts.alertId"), cell: (row) => <span className="font-semibold">{row.alertId}</span> },
    {
      key: "severity",
      header: t("alerts.severity"),
      cell: (row) => <StatusBadge tone={severityTone(row.severity)} label={labels.severity(row.severity)} />,
    },
    {
      key: "component",
      header: t("alerts.component"),
      className: "whitespace-nowrap font-medium",
      cell: (row) => row.componentName || row.componentId || t("common.none"),
    },
    { key: "message", header: t("alerts.message"), className: "min-w-64 text-text-muted" },
    {
      key: "timestamp",
      header: t("alerts.timestamp"),
      className: "whitespace-nowrap text-text-muted tabular-nums",
      cell: (row) => format.dateTime(row.timestamp),
    },
    {
      key: "status",
      header: t("fields.status"),
      cell: (row) => (
        <StatusBadge tone={isStatus(row, "Open") ? "error" : "success"} label={labels.alertStatus(row.status)} />
      ),
    },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("alerts.title")} subtitle={t("alerts.subtitle", { open: openCount, critical: criticalCount })} />

      {error && <InlineAlert>{error}</InlineAlert>}

      <Card
        title={t("alerts.systemAlerts")}
        actions={
          <Tabs
            label={t("alerts.filterLabel")}
            value={filter}
            onChange={setFilter}
            tabs={FILTERS.map((id) => ({ id, label: t(`alerts.filters.${id}`) }))}
          />
        }
      >
        <Table
          caption={t("alerts.systemAlerts")}
          columns={columns}
          rows={rows}
          getRowKey={(row) => row.alertId}
          emptyMessage={t("alerts.empty")}
          actionsHeader={t("alerts.action")}
          rowActions={(alert) =>
            isStatus(alert, "Open") && (
              <Button size="sm" disabled={updatingId === alert.alertId} onClick={() => resolve(alert)}>
                {updatingId === alert.alertId ? t("alerts.resolving") : t("alerts.resolve")}
              </Button>
            )
          }
        />
      </Card>
    </div>
  );
}
