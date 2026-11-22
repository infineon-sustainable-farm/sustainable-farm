import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Archive, Banknote, Bell, Droplets, Package, Scale, Thermometer, Weight } from "lucide-react";
import { KpiCard, PageHeader } from "../../../shared/ui";
import { CROP_STORAGE_BASE } from "../paths";
import {
    fetchAlertsByStatus,
    fetchBatches,
    fetchBatchSummary,
    fetchSensorReadings,
    fetchSensors,
    fetchStorageZones,
} from "../api";
import { ActiveAlertPanel, BatchesMiniTable, SensorsPanel, StorageGauge, TrendChart } from "./DashboardVisuals";
import QueryState from "./QueryState";
import {
    findMatchingBatches,
    getBatchDisplayId,
    getBatchProductLabel,
} from "../utils/batchSearch";
import { EMPTY, currentLocale, formatMoney, formatNumber, formatQuantity } from "../utils/formatters";
import { usePageTitle } from "../utils/usePageTitle";
import { getOverallUsagePercent, withZoneUsage } from "../utils/storageZones";

const PAGE_LINKS = {
    stock: `${CROP_STORAGE_BASE}/batches`,
    weight: `${CROP_STORAGE_BASE}/batches`,
    volume: `${CROP_STORAGE_BASE}/zones`,
    value: `${CROP_STORAGE_BASE}/batches`,
    alerts: `${CROP_STORAGE_BASE}/alerts`,
    climate: `${CROP_STORAGE_BASE}/zones`,
    recovery: `${CROP_STORAGE_BASE}/quality`,
};

function zoneKey(zone) {
    const key = zone?.id ?? zone?.zoneCode ?? zone?.zone_code;
    return key == null ? null : String(key);
}

function batchZoneKey(batch) {
    const key =
        batch.storageZone?.id ??
        batch.storageZoneId ??
        batch.storage_zone_id ??
        batch.storageZone?.zoneCode ??
        batch.storage_zone?.zoneCode ??
        batch.zoneCode;
    return key == null ? null : String(key);
}

function sensorId(sensor) {
    const id = sensor?.id ?? sensor?.sensorId ?? sensor?.sensor_id;
    return id == null ? null : String(id);
}

function readingSensorId(reading) {
    const id =
        sensorId(reading.sensor ?? reading.sensorRef) ?? reading.sensorId ?? reading.sensor_id;
    return id == null ? null : String(id);
}

function readingDate(reading) {
    return reading.measuredAt ?? reading.measured_at ?? null;
}

function deriveDashboardData(
    summary,
    activeAlerts = [],
    batches = [],
    zones = [],
    sensors = [],
    readings = [],
) {
    const safeActiveAlerts = Array.isArray(activeAlerts) ? activeAlerts : [];
    const safeBatches = Array.isArray(batches) ? batches : [];
    const safeZones = Array.isArray(zones) ? zones : [];
    const safeSensors = Array.isArray(sensors) ? sensors : [];
    const safeReadings = Array.isArray(readings) ? readings : [];

    const zoneKeys = new Set(safeZones.map(zoneKey).filter(Boolean));
    const zoneSensors = safeSensors.filter((sensor) => {
        const key =
            sensor.storageZone?.id ??
            sensor.storageZoneId ??
            sensor.storage_zone_id ??
            sensor.storageZone?.zoneCode ??
            sensor.storage_zone?.zoneCode;
        return key != null && zoneKeys.has(String(key));
    });
    const zoneSensorIds = new Set(zoneSensors.map(sensorId).filter(Boolean));
    const latestBySensor = new Map();
    for (const reading of safeReadings) {
        const id = readingSensorId(reading);
        if (!id || !zoneSensorIds.has(id)) continue;
        const current = latestBySensor.get(id);
        if (!current || new Date(readingDate(reading)) > new Date(readingDate(current))) {
            latestBySensor.set(id, reading);
        }
    }

    const latestReadings = [...latestBySensor.values()];
    const average = (field) => {
        const values = latestReadings
            .map((reading) => reading[field])
            .filter((value) => value != null)
            .map(Number);
        return values.length
            ? values.reduce((total, value) => total + value, 0) / values.length
            : null;
    };

    const cutoff = Date.now() - 24 * 60 * 60 * 1000;
    const temperatureTrend = safeReadings
        .filter((reading) => {
            const id = readingSensorId(reading);
            const measuredAt = readingDate(reading);
            return (
                id &&
                zoneSensorIds.has(id) &&
                reading.temperature != null &&
                measuredAt &&
                new Date(measuredAt).getTime() >= cutoff
            );
        })
        .sort((a, b) => new Date(readingDate(a)) - new Date(readingDate(b)));
    const humidityTrend = safeReadings
        .filter((reading) => {
            const id = readingSensorId(reading);
            const measuredAt = readingDate(reading);
            return (
                id &&
                zoneSensorIds.has(id) &&
                reading.humidity != null &&
                measuredAt &&
                new Date(measuredAt).getTime() >= cutoff
            );
        })
        .sort((a, b) => new Date(readingDate(a)) - new Date(readingDate(b)));

    const zonesWithUsage = withZoneUsage(safeZones, safeBatches);
    const overallUsagePercent = getOverallUsagePercent(zonesWithUsage);

    return {
        summary: summary ?? {},
        activeAlertCount: safeActiveAlerts.length,
        activeAlerts: safeActiveAlerts,
        batches: safeBatches,
        zones: zonesWithUsage,
        overallUsagePercent,
        averageTemperature: average("temperature"),
        averageHumidity: average("humidity"),
        temperatureTrend,
        humidityTrend,
        zoneSensors,
        latestReadings,
    };
}

export default function CropStoragePage() {
    const { t } = useTranslation("cropstorage");
    const [now, setNow] = useState(new Date());
    const [batchSearch, setBatchSearch] = useState("");
    const [suggestionsOpen, setSuggestionsOpen] = useState(false);
    const [activeSuggestion, setActiveSuggestion] = useState(-1);
    const batchSearchRef = useRef(null);
    const batchesTableRef = useRef(null);

    usePageTitle("overview.title");

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    const dashboardQuery = useQuery({
        queryKey: ["cropstorage", "dashboard"],
        queryFn: async () => {
            const [summary, activeAlerts, batches, zones, sensors, readings] = await Promise.all([
                fetchBatchSummary(),
                fetchAlertsByStatus("ACTIVE"),
                fetchBatches(),
                fetchStorageZones(),
                fetchSensors(),
                fetchSensorReadings(),
            ]);
            return deriveDashboardData(summary, activeAlerts, batches, zones, sensors, readings);
        },
    });
    const { data } = dashboardQuery;

    const summary = data?.summary ?? {};
    const totalLb =
        summary.totalLb ?? summary.totalLbs ?? summary.totalPounds ?? summary.total_lbs;
    const totalEur = summary.totalValueEur ?? summary.total_value_eur ?? summary.valueEur;
    const totalUsd = summary.totalValueUsd ?? summary.total_value_usd ?? summary.valueUsd;
    const batchSuggestions = useMemo(
        () => findMatchingBatches(data?.batches ?? [], batchSearch, t),
        [data?.batches, batchSearch, t],
    );

    function selectBatch(batch) {
        setBatchSearch(getBatchDisplayId(batch));
        setSuggestionsOpen(false);
        setActiveSuggestion(-1);
        requestAnimationFrame(() => {
            batchesTableRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
    }

    function handleBatchSearchKeyDown(event) {
        if (event.key === "Escape") {
            setSuggestionsOpen(false);
            setActiveSuggestion(-1);
            return;
        }
        if (!suggestionsOpen || !batchSearch.trim()) return;

        if (event.key === "ArrowDown" && batchSuggestions.length > 0) {
            event.preventDefault();
            setActiveSuggestion((current) => (current + 1) % batchSuggestions.length);
        } else if (event.key === "ArrowUp" && batchSuggestions.length > 0) {
            event.preventDefault();
            setActiveSuggestion((current) =>
                current <= 0 ? batchSuggestions.length - 1 : current - 1,
            );
        } else if (event.key === "Enter" && batchSuggestions.length > 0) {
            event.preventDefault();
            selectBatch(batchSuggestions[activeSuggestion] ?? batchSuggestions[0]);
        }
    }

    const zoneClimates = useMemo(() => {
        const map = {};
        const sensors = data?.zoneSensors ?? [];
        const latest = data?.latestReadings ?? [];
        for (const sensor of sensors) {
            const sId = sensorId(sensor);
            const reading = latest.find((r) => readingSensorId(r) === sId);
            const zKey =
                sensor.storageZone?.id ??
                sensor.storageZoneId ??
                sensor.storage_zone_id ??
                sensor.storageZone?.zoneCode ??
                sensor.storage_zone?.zoneCode;
            if (zKey == null || !reading) continue;
            map[String(zKey)] = { temperature: reading.temperature, humidity: reading.humidity };
        }
        return map;
    }, [data]);

    const sensorCards = useMemo(() => {
        const latest = data?.latestReadings ?? [];
        const cutoff = now.getTime() - 24 * 60 * 60 * 1000;
        return (data?.zoneSensors ?? []).map((sensor) => {
            const sId = sensorId(sensor);
            const reading = latest.find((r) => readingSensorId(r) === sId);
            const type = String(sensor.type ?? sensor.sensorType ?? "").toUpperCase();
            const isHumidity = type.includes("HUM");
            const raw = isHumidity ? reading?.humidity : reading?.temperature;
            const online =
                reading != null &&
                readingDate(reading) != null &&
                new Date(readingDate(reading)).getTime() >= cutoff;
            return {
                id: sId,
                label: sensor.name ?? t(isHumidity ? "overview.sensors.humidity" : "overview.sensors.temperature", { id: sId }),
                kind: isHumidity ? "humidity" : "temperature",
                valueText:
                    raw == null
                        ? EMPTY
                        : t(isHumidity ? "percent" : "units.celsius", { value: formatNumber(Number(raw)) }),
                online,
            };
        });
    }, [data, now, t]);

    const activeAlertCount = data?.activeAlertCount ?? 0;
    const recovery = summary.recoveryRate ?? summary.recoveryRatePercent;
    const withUnit = (value, unitKey) => (value == null ? EMPTY : t(unitKey, { value: formatNumber(value) }));
    const cards = [
        {
            id: "stock",
            label: t("overview.kpi.stock"),
            value: formatQuantity(summary.totalKg),
            icon: Package,
            to: PAGE_LINKS.stock,
            hint: t("overview.kpi.stockHint"),
        },
        {
            id: "weight",
            label: t("overview.kpi.weight"),
            value: withUnit(summary.totalTonnes, "units.tonnes"),
            icon: Weight,
            to: PAGE_LINKS.weight,
            hint: totalLb == null ? formatQuantity(summary.totalKg) : `${formatQuantity(summary.totalKg)} · ${t("units.pounds", { value: formatNumber(totalLb) })}`,
        },
        {
            id: "volume",
            label: t("overview.kpi.volume"),
            value: withUnit(summary.totalM3, "units.cubicMeters"),
            icon: Archive,
            to: PAGE_LINKS.volume,
            tone: "warning",
            hint: t("overview.kpi.volumeHint", { value: formatNumber(data?.overallUsagePercent) }),
        },
        {
            id: "value",
            label: t("overview.kpi.value"),
            value: formatMoney(summary.totalValueFcfa),
            icon: Banknote,
            to: PAGE_LINKS.value,
            tone: "warning",
            hint:
                totalEur == null && totalUsd == null
                    ? t("overview.kpi.valueHint")
                    : `${formatNumber(totalEur)} EUR · ${formatNumber(totalUsd)} USD`,
        },
        {
            id: "temperature",
            label: t("overview.kpi.temperature"),
            value: withUnit(data?.averageTemperature, "units.celsius"),
            icon: Thermometer,
            to: PAGE_LINKS.climate,
            tone: "warning",
            hint: t("overview.kpi.temperatureTarget"),
        },
        {
            id: "humidity",
            label: t("overview.kpi.humidity"),
            value: withUnit(data?.averageHumidity, "percent"),
            icon: Droplets,
            to: PAGE_LINKS.climate,
            tone: "info",
            hint: t("overview.kpi.humidityTarget"),
        },
        {
            id: "recovery",
            label: t("overview.kpi.recovery"),
            value: withUnit(recovery, "percent"),
            icon: Scale,
            to: PAGE_LINKS.recovery,
            hint: recovery == null ? t("overview.kpi.recoveryMissing") : t("overview.kpi.recoveryTarget"),
        },
        {
            id: "alerts",
            label: t("overview.kpi.alerts"),
            value: t("overview.kpi.alertCount", { count: activeAlertCount, value: formatNumber(activeAlertCount) }),
            icon: Bell,
            to: PAGE_LINKS.alerts,
            tone: activeAlertCount ? "error" : "primary",
            hint: activeAlertCount ? t("overview.kpi.alertsAttention") : t("overview.kpi.alertsNone"),
        },
    ];

    return (
        <div className="flex flex-col gap-5">
            <PageHeader
                title={t("overview.title")}
                subtitle={t("overview.subtitle")}
                meta={
                    <span className="tabular-nums">
                        {now.toLocaleDateString(currentLocale(), { month: "long", year: "numeric" })}{" "}
                        {now.toLocaleTimeString(currentLocale(), { hour: "numeric", minute: "2-digit" })}
                    </span>
                }
                actions={
                    <div className="flex w-full flex-wrap items-center gap-3 sm:w-auto">
                        <div
                            ref={batchSearchRef}
                            className="relative w-full sm:w-64"
                            onBlur={() => {
                                window.setTimeout(() => {
                                    if (!batchSearchRef.current?.contains(document.activeElement)) {
                                        setSuggestionsOpen(false);
                                        setActiveSuggestion(-1);
                                    }
                                }, 0);
                            }}
                        >
                            <input
                                type="search"
                                value={batchSearch}
                                onChange={(event) => {
                                    setBatchSearch(event.target.value);
                                    setSuggestionsOpen(Boolean(event.target.value.trim()));
                                    setActiveSuggestion(-1);
                                }}
                                onKeyDown={handleBatchSearchKeyDown}
                                onFocus={() => {
                                    if (batchSearch.trim()) setSuggestionsOpen(true);
                                }}
                                placeholder={t("overview.search.placeholder")}
                                aria-label={t("overview.search.label")}
                                role="combobox"
                                aria-autocomplete="list"
                                aria-expanded={suggestionsOpen && Boolean(batchSearch.trim())}
                                aria-controls="cropstorage-batch-search-listbox"
                                aria-activedescendant={
                                    activeSuggestion >= 0 ? `cropstorage-batch-suggestion-${activeSuggestion}` : undefined
                                }
                                className="w-full rounded-control border border-border-input bg-surface-raised px-3 py-2 text-sm text-text focus-visible:outline-2 focus-visible:outline-primary"
                            />
                            {suggestionsOpen && batchSearch.trim() && (
                                <ul
                                    id="cropstorage-batch-search-listbox"
                                    role="listbox"
                                    aria-label={t("overview.search.listLabel")}
                                    className="absolute top-full right-0 left-0 z-(--z-overlay) mt-1 max-h-60 overflow-y-auto rounded-control border border-border-subtle bg-surface-raised py-1 shadow-overlay"
                                >
                                    {batchSuggestions.length === 0 ? (
                                        <li className="px-3 py-2 text-sm text-text-muted" role="status">
                                            {t("overview.search.noMatch")}
                                        </li>
                                    ) : (
                                        batchSuggestions.map((batch, index) => (
                                            <li
                                                id={`cropstorage-batch-suggestion-${index}`}
                                                key={batch.id ?? getBatchDisplayId(batch)}
                                                role="option"
                                                aria-selected={activeSuggestion === index}
                                                onMouseEnter={() => setActiveSuggestion(index)}
                                                onMouseDown={(event) => event.preventDefault()}
                                                onClick={() => selectBatch(batch)}
                                                className={`cursor-pointer px-3 py-2 text-sm ${
                                                    activeSuggestion === index ? "bg-primary-tint text-text" : "text-text hover:bg-surface-muted"
                                                }`}
                                            >
                                                <span className="font-semibold">{getBatchDisplayId(batch)}</span>
                                                <span className="ml-2 text-text-muted">{getBatchProductLabel(batch, t)}</span>
                                            </li>
                                        ))
                                    )}
                                </ul>
                            )}
                        </div>
                        <Link
                            to={PAGE_LINKS.alerts}
                            aria-label={t("overview.openAlerts")}
                            className="relative rounded-control border border-border-input bg-surface-raised p-2 text-text-muted hover:border-primary"
                        >
                            <Bell size={17} aria-hidden="true" />
                            {activeAlertCount ? (
                                <span className="absolute -top-1.5 -right-1.5 flex h-4 min-w-4 items-center justify-center rounded-pill bg-danger px-1 text-xs font-bold text-on-primary">
                                    {activeAlertCount}
                                </span>
                            ) : null}
                        </Link>
                    </div>
                }
            />

            <QueryState query={dashboardQuery} errorKey="overview.loadError" rows={4} />

            {dashboardQuery.isSuccess && (
                <>
                    <section aria-label={t("overview.kpiLabel")} className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                        {cards.map(({ id, ...card }) => (
                            <KpiCard key={id} {...card} />
                        ))}
                    </section>

                    <section className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                        <StorageGauge
                            percentage={data.overallUsagePercent}
                            usedKg={(data.zones ?? []).every((zone) => zone.usedKg != null)
                                ? data.zones.reduce((total, zone) => total + zone.usedKg, 0)
                                : null}
                            capacityKg={(data.zones ?? []).every((zone) => zone.capacityKg != null && zone.capacityKg > 0)
                                ? data.zones.reduce((total, zone) => total + zone.capacityKg, 0)
                                : null}
                            zones={data.zones}
                        />
                        <TrendChart
                            title={t("overview.trend.temperature")}
                            readings={data.temperatureTrend}
                            field="temperature"
                            unit="°C"
                            color="var(--color-chart-2)"
                            targetMin={15}
                            targetMax={20}
                            emptyMessage={t("overview.trend.noTemperature")}
                        />
                        <TrendChart
                            title={t("overview.trend.humidity")}
                            readings={data.humidityTrend}
                            field="humidity"
                            unit="%"
                            color="var(--color-chart-6)"
                            targetMin={50}
                            targetMax={55}
                            emptyMessage={t("overview.trend.noHumidity")}
                        />
                        <ActiveAlertPanel alerts={data.activeAlerts} to={PAGE_LINKS.alerts} />
                    </section>

                    <section className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                        <BatchesMiniTable
                            batches={data.batches}
                            zoneClimates={zoneClimates}
                            zoneKeyOf={batchZoneKey}
                            search={batchSearch}
                            viewAllTo={PAGE_LINKS.stock}
                            tableRef={batchesTableRef}
                        />
                        <SensorsPanel sensors={sensorCards} />
                    </section>
                </>
            )}
        </div>
    );
}
