import { Activity, Gauge, PackageCheck } from "lucide-react";
import { formatNumber, formatQuantity } from "../utils/formatters";

export function KpiCard({ label, value, unit, icon: Icon, badge, caption, captionStyle }) {
    return (
        <article className="rounded-xl border border-line bg-white p-5">
            <div className={`mb-4 inline-flex rounded-xl p-2.5 ${badge}`}>
                <Icon size={19} aria-hidden="true" />
            </div>
            <p className="font-heading text-2xl font-bold text-ink">
                {value}
                {unit && <span className="ml-1 text-sm font-medium text-muted">{unit}</span>}
            </p>
            <p className="mt-1 text-xs font-semibold tracking-wider text-muted uppercase">{label}</p>
            <p className={`mt-3 border-t border-line pt-2 text-xs font-medium ${captionStyle}`}>
                {caption}
            </p>
        </article>
    );
}

export function StorageGauge({ percentage = 0, usedKg = 0, capacityKg = 0, zones = [] }) {
    const safeZones = Array.isArray(zones) ? zones : [];
    const clamped = Math.min(Math.max(percentage, 0), 100);
    const circumference = Math.PI * 80;
    const offset = circumference * (1 - clamped / 100);

    return (
        <section className="rounded-xl border border-line bg-white p-5">
            <div className="flex items-center gap-2">
                <Gauge size={18} className="text-primary" aria-hidden="true" />
                <h2 className="font-heading text-lg font-semibold text-ink">Storage Capacity</h2>
            </div>
            {(safeZones?.length ?? 0) === 0 ? (
                <p className="mt-5 rounded-lg border border-dashed border-line bg-navy-50 px-4 py-8 text-center text-sm text-muted">
                    No storage zones configured. Add zones to track capacity.
                </p>
            ) : (
                <>
                    <div className="relative mx-auto mt-2 h-36 max-w-xs overflow-hidden">
                        <svg
                            viewBox="0 0 200 110"
                            role="img"
                            aria-label={`Overall storage usage ${formatNumber(percentage)} percent`}
                            className="h-full w-full"
                        >
                            <path
                                d="M 20 100 A 80 80 0 0 1 180 100"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="16"
                                strokeLinecap="round"
                                className="text-primary-100"
                            />
                            <path
                                d="M 20 100 A 80 80 0 0 1 180 100"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="16"
                                strokeLinecap="round"
                                strokeDasharray={circumference}
                                strokeDashoffset={offset}
                                className="text-primary"
                            />
                        </svg>
                        <div className="absolute inset-x-0 bottom-0 text-center">
                            <p className="font-heading text-3xl font-bold text-ink">
                                {formatNumber(percentage)}%
                            </p>
                            <p className="text-xs text-muted">
                                {formatQuantity(usedKg)} / {formatQuantity(capacityKg)}
                            </p>
                        </div>
                    </div>
                    <div className="mt-3 flex flex-col gap-3">
                        {safeZones?.map((zone) => {
                            const capacity = Number(zone.capacityKg ?? zone.capacity_kg ?? 0);
                            const used = zone.currentQuantityKg;
                            const usedPercent = capacity > 0 ? (used / capacity) * 100 : 0;
                            return (
                                <div key={zone.id ?? zone.zoneCode} className="flex items-center gap-3">
                                    <span className="w-24 truncate text-xs font-medium text-muted">
                                        {zone.name ?? zone.zoneCode}
                                    </span>
                                    <div
                                        className="h-2 flex-1 overflow-hidden rounded-full bg-primary-50"
                                        role="progressbar"
                                        aria-label={`${zone.name ?? zone.zoneCode} capacity`}
                                        aria-valuemin={0}
                                        aria-valuemax={100}
                                        aria-valuenow={Math.min(usedPercent, 100)}
                                    >
                                        <div
                                            className="h-full rounded-full bg-primary"
                                            style={{ width: `${Math.min(usedPercent, 100)}%` }}
                                        />
                                    </div>
                                    <span className="w-12 text-right text-xs tabular-nums text-muted">
                                        {formatNumber(usedPercent)}%
                                    </span>
                                </div>
                            );
                        }) ?? []}
                    </div>
                </>
            )}
        </section>
    );
}

function getSparklinePoints(readings = []) {
    if (!readings?.length) return "";
    const safeReadings = readings ?? [];
    const values = safeReadings?.map((reading) => Number(reading.temperature)) ?? [];
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    return values
        ?.map((value, index) => {
            const x = safeReadings?.length === 1 ? 200 : (index / ((safeReadings?.length ?? 1) - 1)) * 200;
            const y = 75 - ((value - min) / range) * 60;
            return `${index === 0 ? "M" : "L"} ${x} ${y}`;
        })
        ?.join(" ") ?? "";
}

export function TemperatureSparkline({ readings = [] }) {
    const safeReadings = Array.isArray(readings) ? readings : [];
    const values = safeReadings?.map((reading) => Number(reading.temperature)) ?? [];
    const latest = values.at(-1);

    return (
        <section className="rounded-xl border border-line bg-white p-5">
            <div className="flex items-center gap-2">
                <Activity size={18} className="text-accent-600" aria-hidden="true" />
                <h2 className="font-heading text-lg font-semibold text-ink">Temperature Trend</h2>
            </div>
            {(safeReadings?.length ?? 0) === 0 ? (
                <p className="mt-5 rounded-lg bg-navy-50 px-4 py-8 text-center text-sm text-muted">
                    No temperature readings in the last 24 hours. Readings will appear when sensors report.
                </p>
            ) : (
                <>
                    <div className="mt-4 flex items-baseline gap-2">
                        <p className="font-heading text-3xl font-bold text-ink">
                            {formatNumber(latest)}°C
                        </p>
                        <p className="text-xs text-muted">Latest · last 24 hours</p>
                    </div>
                    <svg
                        viewBox="0 0 200 90"
                        role="img"
                        aria-label={`Temperature trend over 24 hours, ${safeReadings?.length ?? 0} readings`}
                        className="mt-3 h-32 w-full overflow-visible"
                        preserveAspectRatio="none"
                    >
                        <path
                            d="M 0 80 H 200"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="1"
                            className="text-line"
                        />
                        <path
                            d={getSparklinePoints(safeReadings)}
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="3"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            className="text-accent-600"
                        />
                    </svg>
                    <div className="flex justify-between text-xs text-muted">
                        <span>24 hours ago</span>
                        <span>Now</span>
                    </div>
                    <div className="mt-4 flex items-center gap-2 border-t border-line pt-3 text-xs text-muted">
                        <PackageCheck size={15} className="text-success" aria-hidden="true" />
                        {safeReadings?.length ?? 0} readings from sensors in storage zones
                    </div>
                </>
            )}
        </section>
    );
}
