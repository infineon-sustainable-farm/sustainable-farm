import { useTranslation } from "react-i18next";
import { Button, StatusBadge, Table } from "../../../shared/ui";
import { GROUP, enumLabel } from "../utils/enums";
import { EMPTY, formatDateTime } from "../utils/formatters";

const STATUS_TONES = { ACTIVE: "error", ACKNOWLEDGED: "warning", RESOLVED: "success" };

export default function AlertsTable({ alerts, isUpdating, onAcknowledge, onResolve }) {
    const { t } = useTranslation("cropstorage");
    const columns = [
        { key: "id", header: t("alerts.columns.id") },
        { key: "type", header: t("alerts.columns.type"), cell: (a) => enumLabel(t, GROUP.alertType, a.type) },
        { key: "severity", header: t("alerts.columns.severity"), cell: (a) => enumLabel(t, GROUP.severity, a.severity) },
        { key: "message", header: t("alerts.columns.message"), cell: (a) => a.message ?? EMPTY, className: "min-w-48" },
        {
            key: "sensor",
            header: t("alerts.columns.sensor"),
            cell: (a) => a.sensor?.name ?? a.sensor?.sensorCode ?? a.storageZone?.zoneCode ?? EMPTY,
        },
        { key: "createdAt", header: t("alerts.columns.raisedAt"), cell: (a) => formatDateTime(a.createdAt) },
        {
            key: "status",
            header: t("alerts.columns.status"),
            cell: (a) => (
                <StatusBadge tone={STATUS_TONES[a.status] ?? "neutral"} label={enumLabel(t, GROUP.alertStatus, a.status)} withIcon />
            ),
        },
    ];

    return (
        <Table
            columns={columns}
            rows={alerts}
            caption={t("alerts.tableCaption")}
            rowActions={(alert) => (
                <div className="flex gap-2">
                    {alert.status === "ACTIVE" && (
                        <Button size="sm" variant="secondary" onClick={() => onAcknowledge(alert.id)} disabled={isUpdating}>
                            {t("alerts.acknowledge")}
                        </Button>
                    )}
                    {(alert.status === "ACTIVE" || alert.status === "ACKNOWLEDGED") && (
                        <Button size="sm" onClick={() => onResolve(alert.id)} disabled={isUpdating}>
                            {t("alerts.resolve")}
                        </Button>
                    )}
                    {alert.status === "RESOLVED" && <span className="text-xs text-text-muted">{t("alerts.noAction")}</span>}
                </div>
            )}
        />
    );
}
