import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
    AlertTriangle,
    Archive,
    Banknote,
    Bell,
    Droplets,
    Loader2,
    Package,
    Scale,
    Thermometer,
    Weight,
} from "lucide-react";
import {
    fetchAlertsByStatus,
    fetchBatches,
    fetchBatchSummary,
    fetchSensorReadings,
    fetchSensors,
    fetchStorageZones,
} from "../api";
import {
    ActiveAlertPanel,
    BatchesMiniTable,
    KpiCard,
    SensorsPanel,
    StorageGauge,
    TrendChart,
} from "./DashboardVisuals";
import {
    findMatchingBatches,
    getBatchDisplayId,
    getBatchProductLabel,
} from "../utils/batchSearch";
import { formatMoney, formatNumber, formatQuantity } from "../utils/formatters";

const PAGE_LINKS = {
    stock: "batches",
    weight: "batches",
    volume: "zones",
    value: "batches",
    alerts: "alerts",
    climate: "zones",
    recovery: "quality",
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

    const quantities = new Map();
    for (const batch of safeBatches) {
        const key = batchZoneKey(batch);
        if (key === null) continue;
        const quantity = Number(batch.currentQuantityKg ?? batch.current_quantity_kg ?? 0);
        quantities.set(key, (quantities.get(key) ?? 0) + quantity);
    }

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

    const capacityKg = safeZones.reduce(
        (total, zone) => total + Number(zone.capacityKg ?? zone.capacity_kg ?? 0),
        0,
    );
    const usedKg = safeZones.reduce(
        (total, zone) => total + (quantities.get(zoneKey(zone)) ?? 0),
        0,
    );
    const overallUsagePercent = capacityKg > 0 ? (usedKg / capacityKg) * 100 : null;

    return {
        summary: summary ?? {},
        activeAlertCount: safeActiveAlerts.length,
        activeAlerts: safeActiveAlerts,
        batches: safeBatches,
        zones: safeZones.map((zone) => ({
            ...zone,
            currentQuantityKg: quantities.get(zoneKey(zone)) ?? 0,
        })),
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
    const [now, setNow] = useState(new Date());
    const [batchSearch, setBatchSearch] = useState("");
    const [suggestionsOpen, setSuggestionsOpen] = useState(false);
    const [activeSuggestion, setActiveSuggestion] = useState(-1);
    const batchesTableRef = useRef(null);

    useEffect(() => {
        document.title = "Crop Storage";
    }, []);

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    const { data, isPending, isError, refetch, isFetching, error } = useQuery({
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

    const summary = data?.summary ?? {};
    const totalLb =
        summary.totalLb ?? summary.totalLbs ?? summary.totalPounds ?? summary.total_lbs;
    const totalEur = summary.totalValueEur ?? summary.total_value_eur ?? summary.valueEur;
    const totalUsd = summary.totalValueUsd ?? summary.total_value_usd ?? summary.valueUsd;
    const batchSuggestions = useMemo(
        () => findMatchingBatches(data?.batches ?? [], batchSearch),
        [data?.batches, batchSearch],
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
                label: sensor.name ?? `${isHumidity ? "Humidity" : "Temperature"} sensor ${sId}`,
                kind: isHumidity ? "humidity" : "temperature",
                valueText:
                    raw == null ? "—" : `${formatNumber(Number(raw))}${isHumidity ? " %" : " °C"}`,
                online,
            };
        });
    }, [data, now]);

    const cards = [
        {
            label: "Current stock",
            value: formatQuantity(summary.totalKg),
            icon: Package,
            to: PAGE_LINKS.stock,
            badge: "bg-emerald-50 text-emerald-700",
            caption: "Across all batches",
            captionStyle: "text-emerald-700",
        },
        {
            label: "Stock weight",
            value: formatNumber(summary.totalTonnes),
            unit: summary.totalTonnes == null ? "" : "tonnes",
            icon: Weight,
            to: PAGE_LINKS.weight,
            badge: "bg-emerald-50 text-emerald-700",
            caption: `${formatQuantity(summary.totalKg)}${totalLb == null ? "" : ` · ${formatNumber(totalLb)} lb`}`,
            captionStyle: "text-gray-500",
        },
        {
            label: "Stock volume",
            value: formatNumber(summary.totalM3),
            unit: "m³",
            icon: Archive,
            to: PAGE_LINKS.volume,
            badge: "bg-amber-50 text-amber-700",
            caption: `${formatNumber(data?.overallUsagePercent)}% of capacity`,
            captionStyle: "text-emerald-700",
        },
        {
            label: "Inventory value",
            value: formatMoney(summary.totalValueFcfa),
            icon: Banknote,
            to: PAGE_LINKS.value,
            badge: "bg-amber-50 text-amber-700",
            caption:
                totalEur == null && totalUsd == null
                    ? "Total inventory value"
                    : `${formatNumber(totalEur)} EUR · ${formatNumber(totalUsd)} USD`,
            captionStyle: "text-gray-500",
        },
        {
            label: "Avg Temperature",
            value: formatNumber(data?.averageTemperature),
            unit: data?.averageTemperature == null ? "" : "°C",
            icon: Thermometer,
            to: PAGE_LINKS.climate,
            badge: "bg-orange-50 text-orange-700",
            caption: "Target: 15–20 °C",
            captionStyle: "text-emerald-700",
        },
        {
            label: "Avg Humidity",
            value: formatNumber(data?.averageHumidity),
            unit: data?.averageHumidity == null ? "" : "%",
            icon: Droplets,
            to: PAGE_LINKS.climate,
            badge: "bg-sky-50 text-sky-700",
            caption: "Target: 50–55 %",
            captionStyle: "text-emerald-700",
        },
        {
            label: "Recovery Rate",
            value: summary.recoveryRate ?? summary.recoveryRatePercent,
            formatValue: true,
            unit:
                summary.recoveryRate == null && summary.recoveryRatePercent == null ? "" : "%",
            icon: Scale,
            to: PAGE_LINKS.recovery,
            badge: "bg-emerald-50 text-emerald-700",
            caption:
                summary.recoveryRate == null && summary.recoveryRatePercent == null
                    ? "Not reported by summary API"
                    : "Target: at least 80%",
            captionStyle: "text-gray-500",
        },
        {
            label: "Active alerts",
            value: formatNumber(data?.activeAlertCount),
            unit: "alerts",
            icon: Bell,
            to: PAGE_LINKS.alerts,
            badge: data?.activeAlertCount
                ? "bg-red-50 text-red-700"
                : "bg-emerald-50 text-emerald-700",
            caption: data?.activeAlertCount ? "Requires attention" : "No active alerts",
            captionStyle: data?.activeAlertCount ? "text-red-600" : "text-emerald-700",
        },
    ];

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-7xl flex-col gap-5 px-4 py-6 sm:px-6">
                <header className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">Crop Storage Dashboard</h1>
                        <p className="text-sm text-gray-500">Farm Sustainable Management System</p>
                    </div>
                    <div className="flex w-full flex-wrap items-center gap-4 sm:w-auto">
                        <div className="relative w-full sm:w-64">
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
                                placeholder="Search by ID or product"
                                aria-label="Filter stored batches"
                                role="combobox"
                                aria-autocomplete="list"
                                aria-expanded={suggestionsOpen && Boolean(batchSearch.trim())}
                                aria-controls="cropstorage-batch-search-listbox"
                                aria-activedescendant={
                                    activeSuggestion >= 0
                                        ? `cropstorage-batch-suggestion-${activeSuggestion}`
                                        : undefined
                                }
                                className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-emerald-600 focus:outline-none"
                            />
                            {suggestionsOpen && batchSearch.trim() && (
                                <ul
                                    id="cropstorage-batch-search-listbox"
                                    role="listbox"
                                    aria-label="Matching Crop Storage batches"
                                    className="absolute top-full right-0 left-0 z-50 mt-1 max-h-60 overflow-y-auto rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
                                >
                                    {batchSuggestions.length === 0 ? (
                                        <li className="px-3 py-2 text-sm text-gray-500" role="status">
                                            No matching batches.
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
                                                    activeSuggestion === index
                                                        ? "bg-emerald-50 text-emerald-900"
                                                        : "text-gray-700 hover:bg-gray-50"
                                                }`}
                                            >
                                                <span className="font-semibold">{getBatchDisplayId(batch)}</span>
                                                <span className="ml-2 text-gray-500">
                                                    {getBatchProductLabel(batch)}
                                                </span>
                                            </li>
                                        ))
                                    )}
                                </ul>
                            )}
                        </div>
                        <span className="text-sm text-gray-500">
                            {now.toLocaleDateString("en-GB", { month: "long", year: "numeric" })}
                        </span>
                        <span className="text-sm font-semibold text-gray-900 tabular-nums">
                            {now.toLocaleTimeString("en-GB", { hour: "numeric", minute: "2-digit" })}
                        </span>
                        <Link
                            to={PAGE_LINKS.alerts}
                            aria-label="Open alerts"
                            className="relative rounded-lg border border-gray-200 bg-white p-2 text-gray-600 hover:border-emerald-600"
                        >
                            <Bell size={17} aria-hidden="true" />
                            {data?.activeAlertCount ? (
                                <span className="absolute -top-1.5 -right-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white">
                                    {data.activeAlertCount}
                                </span>
                            ) : null}
                        </Link>
                    </div>
                </header>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading storage dashboard…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-xl border border-red-200 bg-red-50 px-6 py-16 text-center">
                        <AlertTriangle size={28} className="text-red-600" />
                        <div>
                            <p className="text-base font-bold text-gray-900">
                                Crop Storage dashboard could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-gray-500">{error.message}</p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <section aria-label="Inventory key performance indicators">
                            <div
                                style={{
                                    display: "grid",
                                    gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
                                    gap: "12px",
                                }}
                            >
                                {cards.map((card) => (
                                    <KpiCard
                                        key={card.label}
                                        {...card}
                                        value={card.formatValue ? formatNumber(card.value) : card.value}
                                    />
                                ))}
                            </div>
                        </section>

                        <section
                            style={{
                                display: "grid",
                                gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                                gap: "16px",
                            }}
                        >
                            <StorageGauge
                                percentage={data.overallUsagePercent}
                                usedKg={(data.zones ?? []).reduce(
                                    (total, zone) => total + zone.currentQuantityKg,
                                    0,
                                )}
                                capacityKg={data.zones.reduce(
                                    (total, zone) =>
                                        total + Number(zone.capacityKg ?? zone.capacity_kg ?? 0),
                                    0,
                                )}
                                zones={data.zones}
                            />
                            <TrendChart
                                title="Temperature Trend"
                                readings={data.temperatureTrend}
                                field="temperature"
                                unit="°C"
                                color="#c2630b"
                                targetMin={15}
                                targetMax={20}
                                emptyMessage="No temperature readings in the last 24 hours. Readings will appear when sensors report."
                            />
                            <TrendChart
                                title="Humidity Trend"
                                readings={data.humidityTrend}
                                field="humidity"
                                unit="%"
                                color="#0284c7"
                                targetMin={50}
                                targetMax={55}
                                emptyMessage="No humidity readings in the last 24 hours. Readings will appear when sensors report."
                            />
                            <ActiveAlertPanel alerts={data.activeAlerts} to={PAGE_LINKS.alerts} />
                        </section>

                        <section
                            style={{
                                display: "grid",
                                gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
                                gap: "16px",
                            }}
                        >
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
        </div>
    );
}
