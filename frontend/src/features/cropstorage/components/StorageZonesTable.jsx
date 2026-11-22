import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { GROUP, enumLabel } from "../utils/enums";
import {
    CAPACITY_BAR_CLASSES,
    EMPTY,
    capacityLevel,
    formatNumber,
    formatPercentage,
    formatQuantity,
} from "../utils/formatters";

function UsageBar({ zone, t }) {
    const percent = zone.usedPercent;
    const hasPercent = Number.isFinite(percent);
    const label = zone.zoneCode ?? zone.name;
    return (
        <div className="flex min-w-40 items-center gap-3">
            <div
                className="h-2.5 min-w-20 flex-1 overflow-hidden rounded-pill bg-neutral-bg"
                role="progressbar"
                aria-label={t("zones.usageLabel", { zone: label })}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={hasPercent ? Math.min(percent, 100) : undefined}
                aria-valuetext={
                    hasPercent ? t("zones.usedValue", { value: formatPercentage(percent) }) : t("zones.usageUnavailable")
                }
            >
                <div
                    className={`h-full rounded-pill ${CAPACITY_BAR_CLASSES[capacityLevel(percent)]}`}
                    style={{ width: `${hasPercent ? Math.min(percent, 100) : 0}%` }}
                />
            </div>
            <span className="w-16 text-right tabular-nums text-text-muted">
                {hasPercent ? t("percent", { value: formatPercentage(percent) }) : EMPTY}
            </span>
        </div>
    );
}

export default function StorageZonesTable({ zones }) {
    const { t } = useTranslation("cropstorage");
    const columns = [
        { key: "zoneCode", header: t("zones.columns.code"), cell: (zone) => zone.zoneCode ?? EMPTY },
        { key: "name", header: t("zones.columns.name"), cell: (zone) => zone.name || EMPTY },
        { key: "status", header: t("zones.columns.status"), cell: (zone) => enumLabel(t, GROUP.operationalStatus, zone.status) },
        {
            key: "capacityKg",
            header: t("zones.columns.capacity"),
            align: "right",
            cell: (zone) => formatQuantity(zone.capacityKg),
        },
        {
            key: "capacityM3",
            header: t("zones.columns.capacityM3"),
            align: "right",
            cell: (zone) => t("units.cubicMeters", { value: formatNumber(zone.capacityM3) }),
        },
        { key: "used", header: t("zones.columns.used"), cell: (zone) => <UsageBar zone={zone} t={t} /> },
    ];
    return (
        <Table
            columns={columns}
            rows={zones}
            getRowKey={(zone) => zone.id ?? zone.zoneCode}
            caption={t("zones.tableCaption")}
        />
    );
}
