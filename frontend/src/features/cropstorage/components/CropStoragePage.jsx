import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import {
    Archive,
    Banknote,
    Bell,
    Droplets,
    Package,
    Scale,
    Thermometer,
    Weight,
} from "lucide-react";
import { AlertTriangle, Loader2 } from "lucide-react";
import {
    fetchAlertsByStatus,
    fetchBatches,
    fetchBatchSummary,
    fetchSensorReadings,
    fetchSensors,
    fetchStorageZones,
} from "../api";
import { KpiCard, StorageGauge, TemperatureSparkline } from "./DashboardVisuals";

const numberFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 });

function formatNumber(value) {
    return value == null || value === "" ? "—" : numberFormat.format(Number(value));
}

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
    const id = sensorId(reading.sensor ?? reading.sensorRef) ??
        reading.sensorId ??
        reading.sensor_id;
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

    const capacityKg = safeZones.reduce(
        (total, zone) => total + Number(zone.capacityKg ?? zone.capacity_kg ?? 0),
        0,
    );
    const usedKg = safeZones.reduce(
        (total, zone) => total + (quantities.get(zoneKey(zone)) ?? 0),
        0,
    );
    const overallUsagePercent = capacityKg > 0 ? (usedKg / capacityKg) * 100 : 0;

    return {
        summary: summary ?? {},
        activeAlertCount: safeActiveAlerts.length,
        batches: safeBatches,
        zones: safeZones.map((zone) => ({
            ...zone,
            currentQuantityKg: quantities.get(zoneKey(zone)) ?? 0,
        })),
        overallUsagePercent,
        averageTemperature: average("temperature"),
        averageHumidity: average("humidity"),
        temperatureTrend: Array.isArray(temperatureTrend) ? temperatureTrend : [],
    };
}

export default function CropStoragePage() {
    useEffect(() => {
        document.title = "Crop Storage";
    }, []);

    const { data, isPending, isError, refetch, isFetching } = useQuery({
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
    const cards = [
        {
            label: "Current stock",
            value: formatNumber(summary.totalKg),
            unit: "kg",
            icon: Package,
            badge: "bg-primary-50 text-primary-700",
            caption: "Across all batches",
            captionStyle: "text-primary-700",
        },
        {
            label: "Stock weight",
            value: formatNumber(summary.totalTonnes),
            unit: "tonnes",
            icon: Weight,
            badge: "bg-primary-50 text-primary-700",
            caption: `${formatNumber(summary.totalLb)} lb total`,
            captionStyle: "text-muted",
        },
        {
            label: "Stock volume",
            value: formatNumber(summary.totalM3),
            unit: "m³",
            icon: Archive,
            badge: "bg-accent-50 text-accent-700",
            caption: `${formatNumber(data?.overallUsagePercent)}% of capacity`,
            captionStyle: "text-success",
        },
        {
            label: "Inventory value",
            value: formatNumber(summary.totalValueFcfa),
            unit: "FCFA",
            icon: Banknote,
            badge: "bg-accent-50 text-accent-700",
            caption: `${formatNumber(summary.totalValueEur)} EUR · ${formatNumber(summary.totalValueUsd)} USD`,
            captionStyle: "text-muted",
        },
        {
            label: "Active alerts",
            value: formatNumber(data?.activeAlertCount),
            unit: "alerts",
            icon: Bell,
            badge: data?.activeAlertCount
                ? "bg-error/5 text-error"
                : "bg-success/5 text-success",
            caption: data?.activeAlertCount ? "Requires attention" : "No active alerts",
            captionStyle: data?.activeAlertCount ? "text-error" : "text-success",
        },
        {
            label: "Avg Temperature",
            value: formatNumber(data?.averageTemperature),
            unit: data?.averageTemperature == null ? "" : "°C",
            icon: Thermometer,
            badge: "bg-accent-50 text-accent-700",
            caption: "Target: 15–20 °C",
            captionStyle: "text-muted",
        },
        {
            label: "Avg Humidity",
            value: formatNumber(data?.averageHumidity),
            unit: data?.averageHumidity == null ? "" : "%",
            icon: Droplets,
            badge: "bg-primary-50 text-primary-700",
            caption: "Latest readings in storage zones",
            captionStyle: "text-muted",
        },
        {
            label: "Recovery Rate",
            value: summary.recoveryRate ?? summary.recoveryRatePercent,
            formatValue: true,
            unit:
                summary.recoveryRate == null && summary.recoveryRatePercent == null
                    ? ""
                    : "%",
            icon: Scale,
            badge: "bg-success/5 text-success",
            caption:
                summary.recoveryRate == null && summary.recoveryRatePercent == null
                    ? "Not reported by summary API"
                    : "Batch recovery",
            captionStyle: "text-muted",
        },
    ];

    return (
        <div className="min-h-dvh bg-navy-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-ink">
                        Crop Storage Dashboard
                    </h1>
                    <p className="mt-2 max-w-xl text-sm text-muted">
                        Inventory overview, alerts, and storage conditions.
                    </p>
                </header>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading storage dashboard…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <AlertTriangle size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Crop Storage dashboard could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                One or more requests to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-dark disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <section aria-label="Inventory key performance indicators">
                            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                {cards.map((card) => (
                                    <KpiCard
                                        key={card.label}
                                        {...card}
                                        value={
                                            card.formatValue
                                                ? formatNumber(card.value)
                                                : card.value
                                        }
                                    />
                                ))}
                            </div>
                        </section>

                        <section className="grid gap-4 lg:grid-cols-2">
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
                            <TemperatureSparkline readings={data.temperatureTrend} />
                        </section>

                        {data.zones.length === 0 && (
                            <div className="rounded-xl border border-dashed border-line bg-white px-6 py-8 text-center text-sm text-muted">
                                No storage zones are configured yet.
                            </div>
                        )}
                        {data.batches.length === 0 && (
                            <div className="rounded-xl border border-dashed border-line bg-white px-5 py-4 text-sm text-muted">
                                No batches are recorded in inventory yet.
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
