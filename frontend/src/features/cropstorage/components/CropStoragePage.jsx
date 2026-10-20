import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Loader2, Warehouse } from "lucide-react";
import {
    fetchAlertsByStatus,
    fetchBatches,
    fetchBatchSummary,
    fetchStorageZones,
} from "../api";

const numberFormat = new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 2,
});

function formatNumber(value) {
    return numberFormat.format(Number(value ?? 0));
}

function zoneKey(zone) {
    const key = zone?.id ?? zone?.zoneCode;
    return key == null ? null : String(key);
}

function batchZoneKey(batch) {
    const key =
        batch.storageZone?.id ??
        batch.storageZoneId ??
        batch.storageZone?.zoneCode ??
        batch.zoneCode;
    return key == null ? null : String(key);
}

function getZoneQuantities(zones, batches) {
    const quantities = new Map();
    for (const batch of batches) {
        const key = batchZoneKey(batch);
        if (key === null) continue;
        quantities.set(key, (quantities.get(key) ?? 0) + Number(batch.currentQuantityKg ?? 0));
    }
    return zones.map((zone) => ({
        ...zone,
        currentQuantityKg: quantities.get(zoneKey(zone)) ?? 0,
    }));
}

function KpiCard({ label, value, unit, secondary }) {
    return (
        <article className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-gray-500">{label}</p>
            <p className="font-heading mt-2 text-2xl font-bold text-gray-900">
                {formatNumber(value)}
                {unit && <span className="ml-1 text-sm font-medium text-gray-500">{unit}</span>}
            </p>
            {secondary && (
                <p className="mt-2 text-xs text-gray-500">
                    {formatNumber(secondary.eur)} EUR <span className="mx-1">·</span>{" "}
                    {formatNumber(secondary.usd)} USD
                </p>
            )}
        </article>
    );
}

function StorageZoneCapacity({ zones }) {
    if (zones.length === 0) {
        return (
            <div className="rounded-xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center">
                <Warehouse size={26} className="mx-auto text-gray-400" aria-hidden="true" />
                <p className="mt-3 font-medium text-gray-700">No storage zones configured</p>
                <p className="mt-1 text-sm text-gray-500">
                    Storage capacity will appear here when zones are added.
                </p>
            </div>
        );
    }

    return (
        <div className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="mb-5">
                <h2 className="font-heading text-lg font-semibold text-gray-900">
                    Storage zone capacity
                </h2>
                <p className="mt-1 text-sm text-gray-500">
                    Current batch stock compared with each zone&apos;s capacity.
                </p>
            </div>

            <div className="flex flex-col gap-5">
                {zones.map((zone) => {
                    const capacity = Number(zone.capacityKg ?? 0);
                    const current = zone.currentQuantityKg;
                    const percentage = capacity > 0 ? (current / capacity) * 100 : 0;
                    const barWidth = Math.min(percentage, 100);
                    const overCapacity = percentage > 100;

                    return (
                        <section
                            key={zone.id ?? zone.zoneCode}
                            aria-label={`${zone.name || zone.zoneCode} capacity`}
                        >
                            <div className="mb-2 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                                <p className="font-medium text-gray-800">
                                    {zone.name || zone.zoneCode}
                                    {zone.name && zone.zoneCode && (
                                        <span className="ml-2 text-xs font-normal text-gray-500">
                                            {zone.zoneCode}
                                        </span>
                                    )}
                                </p>
                                <p className={`text-sm ${overCapacity ? "font-semibold text-error" : "text-gray-600"}`}>
                                    {formatNumber(current)} / {formatNumber(capacity)} kg
                                    <span className="ml-2 text-xs">
                                        ({formatNumber(percentage)}%)
                                    </span>
                                </p>
                            </div>
                            <div
                                className="h-3 overflow-hidden rounded-full bg-gray-100"
                                role="progressbar"
                                aria-label={`${zone.name || zone.zoneCode} capacity used`}
                                aria-valuemin={0}
                                aria-valuemax={100}
                                aria-valuenow={Math.min(percentage, 100)}
                                aria-valuetext={`${formatNumber(current)} of ${formatNumber(capacity)} kg, ${formatNumber(percentage)} percent`}
                            >
                                <div
                                    className={`h-full rounded-full transition-[width] ${
                                        overCapacity ? "bg-error" : "bg-primary"
                                    }`}
                                    style={{ width: `${barWidth}%` }}
                                />
                            </div>
                        </section>
                    );
                })}
            </div>
        </div>
    );
}

export default function CropStoragePage() {
    useEffect(() => {
        document.title = "Crop Storage";
    }, []);

    const {
        data,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useQuery({
        queryKey: ["cropstorage", "dashboard"],
        queryFn: async () => {
            const [summary, activeAlerts, batches, zones] = await Promise.all([
                fetchBatchSummary(),
                fetchAlertsByStatus("ACTIVE"),
                fetchBatches(),
                fetchStorageZones(),
            ]);
            return {
                summary,
                activeAlertCount: activeAlerts.length,
                batches,
                zones: getZoneQuantities(zones, batches),
            };
        },
    });

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                        Crop Storage Dashboard
                    </h1>
                    <p className="mt-2 max-w-xl text-sm text-gray-500">
                        Inventory overview, active alerts, and storage capacity.
                    </p>
                </header>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading storage dashboard…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <AlertTriangle size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-gray-900">
                                Crop Storage dashboard could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-gray-500">
                                One or more requests to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <section aria-label="Inventory key performance indicators">
                            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                                <KpiCard label="Current stock" value={data.summary.totalKg} unit="kg" />
                                <KpiCard label="Stock weight" value={data.summary.totalTonnes} unit="tonnes" />
                                <KpiCard label="Stock weight" value={data.summary.totalLb} unit="lb" />
                                <KpiCard label="Stock volume" value={data.summary.totalM3} unit="m³" />
                                <KpiCard
                                    label="Inventory value"
                                    value={data.summary.totalValueFcfa}
                                    unit="FCFA"
                                    secondary={{
                                        eur: data.summary.totalValueEur,
                                        usd: data.summary.totalValueUsd,
                                    }}
                                />
                                <KpiCard label="Active alerts" value={data.activeAlertCount} />
                            </div>
                        </section>

                        {data.batches.length === 0 && (
                            <div className="rounded-xl border border-dashed border-gray-300 bg-white px-5 py-4 text-sm text-gray-600">
                                No batches are recorded in inventory yet.
                            </div>
                        )}

                        <StorageZoneCapacity zones={data.zones} />
                    </>
                )}
            </div>
        </div>
    );
}
