import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, Droplets, Gauge, Thermometer, Radio } from "lucide-react";
import { formatNumber, formatQuantity } from "../utils/formatters";
import {
    findMatchingBatches,
    getBatchDisplayId,
    getBatchProductLabel,
} from "../utils/batchSearch";

export function KpiCard({ label, value, unit, icon: Icon, badge, caption, captionStyle, to }) {
    const inner = (
        <>
            <div className={`mb-3 inline-flex rounded-lg p-2 ${badge}`}>
                <Icon size={18} aria-hidden="true" />
            </div>
            <p className="text-2xl font-bold text-gray-900">
                {value}
                {unit && <span className="ml-1 text-sm font-medium text-gray-500">{unit}</span>}
            </p>
            <p className="mt-1 text-xs font-semibold tracking-wider text-gray-500 uppercase">{label}</p>
            <p className={`mt-2 text-xs font-medium ${captionStyle}`}>{caption}</p>
        </>
    );

    const classes = to
        ? "block rounded-xl border border-gray-200 bg-white p-4 transition hover:border-emerald-600 hover:shadow-md focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600"
        : "rounded-xl border border-gray-200 bg-white p-4";

    return to ? (
        <Link to={to} className={classes} aria-label={`${label}: open the related page`}>
            {inner}
        </Link>
    ) : (
        <article className={classes}>{inner}</article>
    );
}

function getCapacityColor(percent) {
    if (!Number.isFinite(percent)) return "#dbe4ec";
    if (percent < 70) return "#16a34a";
    if (percent <= 90) return "#f59e0b";
    return "#ef4444";
}

export function StorageGauge({ percentage = 0, usedKg = 0, capacityKg = 0, zones = [] }) {
    const rawPercentage = Number(percentage);
    const hasPercentage = Number.isFinite(rawPercentage) && rawPercentage >= 0;
    const displayPercentage = hasPercentage ? rawPercentage : null;
    const clampedPercentage = displayPercentage == null ? 0 : Math.min(Math.max(displayPercentage, 0), 100);
    const [shown, setShown] = useState(0);

    useEffect(() => {
        const id = requestAnimationFrame(() => setShown(clampedPercentage));
        return () => cancelAnimationFrame(id);
    }, [clampedPercentage]);

    const arcColor = getCapacityColor(displayPercentage == null ? 0 : displayPercentage);
    const numericUsed = Number(usedKg ?? 0);
    const numericCapacity = Number(capacityKg ?? 0);
    const hasCapacity = Number.isFinite(numericCapacity) && numericCapacity > 0;
    const percentageText = displayPercentage == null ? "—" : `${formatNumber(clampedPercentage)}%`;
    const capacityText = hasCapacity
        ? `${formatQuantity(numericUsed)} / ${formatQuantity(numericCapacity)}`
        : "Capacity data unavailable";

    return (
        <section className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="flex items-center gap-2">
                <Gauge size={18} color={arcColor} aria-hidden="true" />
                <h2 className="text-lg font-semibold text-gray-900">Storage Capacity</h2>
            </div>
            {(!zones || zones.length === 0) ? (
                <p className="mt-5 rounded-lg border border-dashed border-gray-300 bg-gray-50 px-4 py-8 text-center text-sm text-gray-500">
                    No storage zones configured. Add zones to track capacity.
                </p>
            ) : (
                <>
                    <div className="relative mx-auto mt-3 max-w-[18rem]">
                        <svg
                            viewBox="0 0 220 130"
                            role="img"
                            aria-label={displayPercentage == null ? "Overall storage usage unavailable" : `Overall storage usage ${formatNumber(clampedPercentage)} percent`}
                            className="block h-40 w-full"
                        >
                            <path
                                d="M 30 100 A 80 80 0 0 1 190 100"
                                fill="none"
                                stroke="#dfece7"
                                strokeWidth="18"
                                strokeLinecap="round"
                                pathLength="100"
                            />
                            <path
                                d="M 30 100 A 80 80 0 0 1 190 100"
                                fill="none"
                                stroke={arcColor}
                                strokeWidth="18"
                                strokeLinecap="round"
                                pathLength="100"
                                strokeDasharray="100 100"
                                strokeDashoffset={displayPercentage == null ? 100 : 100 - shown}
                                style={{ transition: "stroke-dashoffset 700ms ease" }}
                            />
                        </svg>
                        <div className="absolute inset-x-0 bottom-0 px-2 text-center">
                            <p className="whitespace-nowrap text-3xl font-bold leading-none tracking-tight text-gray-900">
                                {percentageText}
                            </p>
                            <p className="mt-1 whitespace-nowrap text-sm font-medium text-gray-500">
                                {capacityText}
                            </p>
                        </div>
                    </div>
                    <div className="mt-3 flex flex-col gap-3">
                        {zones.map((zone) => {
                            const capacity = Number(zone.capacityKg ?? zone.capacity_kg ?? 0);
                            const used = Number(zone.currentQuantityKg ?? 0);
                            const zonePercent = capacity > 0 ? (used / capacity) * 100 : null;
                            const zoneDisplay = zonePercent == null ? 0 : Math.min(Math.max(zonePercent, 0), 100);
                            const zoneColor = getCapacityColor(zonePercent);
                            return (
                                <div key={zone.id ?? zone.zoneCode} className="flex items-center gap-3">
                                    <span className="w-24 truncate text-xs font-medium text-gray-500">
                                        {zone.name ?? zone.zoneCode}
                                    </span>
                                    <div
                                        className="h-2 flex-1 overflow-hidden rounded-full"
                                        style={{ background: "#e4f0ea" }}
                                        role="progressbar"
                                        aria-label={`${zone.name ?? zone.zoneCode} capacity`}
                                        aria-valuemin={0}
                                        aria-valuemax={100}
                                        aria-valuenow={zonePercent == null ? 0 : Math.min(zonePercent, 100)}
                                    >
                                        <div
                                            className="h-full rounded-full"
                                            style={{
                                                width: `${zonePercent == null ? 0 : Math.min(zonePercent, 100)}%`,
                                                background: zoneColor,
                                            }}
                                        />
                                    </div>
                                    <span className="w-12 text-right text-xs tabular-nums text-gray-500">
                                        {zonePercent == null ? "—" : `${formatNumber(zoneDisplay)}%`}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                </>
            )}
        </section>
    );
}

export function TrendChart({ title, readings = [], field, unit, color, targetMin, targetMax, emptyMessage }) {
    const values = readings.map((r) => Number(r[field])).filter((v) => !Number.isNaN(v));

    const width = 260;
    const height = 110;
    let path = "";
    let area = "";
    let targetY = null;
    let latest = null;

    if (values.length > 0) {
        latest = values[values.length - 1];
        const low = Math.min(...values, targetMin ?? Infinity);
        const high = Math.max(...values, targetMax ?? -Infinity);
        const range = high - low || 1;
        const stepX = values.length === 1 ? width : width / (values.length - 1);
        const toY = (v) => 10 + (1 - (v - low) / range) * (height - 25);
        path = values
            .map((v, i) => `${i === 0 ? "M" : "L"} ${i * stepX} ${toY(v).toFixed(1)}`)
            .join(" ");
        area = `${path} L ${((values.length - 1) * stepX).toFixed(1)} ${height} L 0 ${height} Z`;
        if (targetMax != null) targetY = toY(targetMax);
    }

    return (
        <section className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="flex items-baseline justify-between">
                <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
                <span className="text-xs text-gray-400">Last 24 hours</span>
            </div>
            {values.length === 0 ? (
                <p className="mt-4 rounded-lg bg-gray-50 px-4 py-8 text-center text-sm text-gray-500">
                    {emptyMessage}
                </p>
            ) : (
                <>
                    <p className="mt-3 text-2xl font-bold text-gray-900">
                        {formatNumber(latest)}
                        <span className="ml-1 text-sm font-medium text-gray-500">{unit}</span>
                    </p>
                    <svg viewBox={`0 0 ${width} ${height}`} className="mt-2 h-32 w-full" preserveAspectRatio="none">
                        <path d={area} fill={color} fillOpacity="0.15" stroke="none" />
                        {targetY != null && (
                            <line x1="0" y1={targetY} x2={width} y2={targetY} stroke={color} strokeWidth="1" strokeDasharray="4 3" opacity="0.7" />
                        )}
                        <path d={path} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    <div className="mt-1 flex justify-between text-xs text-gray-400">
                        <span>24 hours ago</span>
                        <span>Now</span>
                        {targetMax != null && <span>Target {targetMin}–{targetMax}{unit}</span>}
                    </div>
                </>
            )}
        </section>
    );
}

export function ActiveAlertPanel({ alerts = [], to }) {
    const first = alerts[0] ?? null;
    return (
        <section className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="flex items-center gap-2">
                <BellIcon hasAlert={Boolean(first)} />
                <h2 className="text-lg font-semibold text-gray-900">Active Alert</h2>
            </div>
            {!first ? (
                <div className="mt-4 rounded-lg bg-emerald-50 px-4 py-6 text-center">
                    <p className="text-sm font-semibold text-emerald-700">No active alerts</p>
                    <p className="mt-1 text-xs text-gray-500">Sensor thresholds are monitored continuously.</p>
                </div>
            ) : (
                <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-4">
                    <p className="text-sm font-bold text-red-700">
                        {first.type ?? first.alertType ?? "Sensor threshold"} detected
                    </p>
                    <p className="mt-1 text-xs text-gray-700">
                        {first.message ?? "Threshold crossed in a storage zone."}
                    </p>
                    <p className="mt-2 text-xs text-gray-500">
                        {new Date(first.createdAt ?? first.triggeredAt ?? first.created_at).toLocaleString("en-GB")}
                    </p>
                    <Link
                        to={to}
                        className="mt-3 inline-block rounded-lg bg-red-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-red-700"
                    >
                        View details
                    </Link>
                </div>
            )}
        </section>
    );
}

function BellIcon({ hasAlert }) {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={hasAlert ? "#c0392b" : "#0d7a5f"} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
    );
}

export function BatchesMiniTable({
    batches = [],
    zoneClimates = {},
    zoneKeyOf,
    search = "",
    viewAllTo,
    tableRef,
}) {
    const rows = findMatchingBatches(batches, search).slice(0, 5);

    return (
        <section
            ref={tableRef}
            className="rounded-xl border border-gray-200 bg-white p-5 lg:col-span-2"
            style={{ minWidth: 0 }}
        >
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-gray-900">Stored Batches</h2>
                <Link to={viewAllTo} className="text-xs font-semibold text-emerald-700 hover:underline">
                    View all batches →
                </Link>
            </div>
            <div className="mt-3 overflow-x-auto">
                <table className="w-full text-left text-xs">
                    <thead>
                        <tr className="border-b border-gray-100 text-gray-400 uppercase">
                            <th className="py-2 pr-3 font-semibold">Batch ID</th>
                            <th className="py-2 pr-3 font-semibold">Product</th>
                            <th className="py-2 pr-3 font-semibold">Quantity</th>
                            <th className="py-2 pr-3 font-semibold">Temp</th>
                            <th className="py-2 pr-3 font-semibold">Humidity</th>
                            <th className="py-2 pr-3 font-semibold">Batch status</th>
                            <th className="py-2 font-semibold">Last updated</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.length === 0 ? (
                            <tr>
                                <td colSpan="7" className="py-6 text-center text-gray-400">
                                    No batch matches this search.
                                </td>
                            </tr>
                        ) : (
                            rows.map((batch) => {
                                const zoneKey = zoneKeyOf(batch);
                                const climate = zoneKey == null ? null : zoneClimates[zoneKey];
                                const temp = climate?.temperature;
                                const hum = climate?.humidity;
                                const statusValue = batch.status ?? "—";
                                const statusLabel = (typeof statusValue === "string" && statusValue.trim().length > 0)
                                    ? statusValue
                                    : "—";
                                return (
                                    <tr key={batch.id ?? batch.batchCode} className="border-b border-gray-50">
                                        <td className="py-2 pr-3 font-semibold text-gray-800">
                                            {getBatchDisplayId(batch)}
                                        </td>
                                        <td className="py-2 pr-3 text-gray-600">{getBatchProductLabel(batch)}</td>
                                        <td className="py-2 pr-3 text-gray-600">
                                            {formatQuantity(batch.currentQuantityKg ?? batch.current_quantity_kg)}
                                        </td>
                                        <td className="py-2 pr-3 text-gray-600">{temp == null ? "—" : `${formatNumber(temp)} °C`}</td>
                                        <td className="py-2 pr-3 text-gray-600">{hum == null ? "—" : `${formatNumber(hum)} %`}</td>
                                        <td className="py-2 pr-3">
                                            <span
                                                className="inline-block rounded-full px-2 py-0.5 text-xs font-semibold"
                                                style={
                                                    statusLabel === "—"
                                                        ? { background: "#f3f4f6", color: "#6b7280" }
                                                        : statusLabel.toLowerCase().includes("risk") || statusLabel.toLowerCase().includes("late") || statusLabel.toLowerCase().includes("delay")
                                                          ? { background: "#fee2e2", color: "#b91c1c" }
                                                          : { background: "#dcfce7", color: "#166534" }
                                                }
                                            >
                                                {statusLabel}
                                            </span>
                                        </td>
                                        <td className="py-2 text-gray-400">{batchUpdated(batch)}</td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

function batchUpdated(batch) {
    const raw = batch.updatedAt ?? batch.updated_at ?? batch.lastModified;
    if (!raw) return "—";
    return new Date(raw).toLocaleString("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export function SensorsPanel({ sensors = [] }) {
    return (
        <section className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="flex items-center gap-2">
                <Radio size={18} color="#0d7a5f" aria-hidden="true" />
                <h2 className="text-lg font-semibold text-gray-900">Environmental Sensors</h2>
            </div>
            {sensors.length === 0 ? (
                <p className="mt-4 rounded-lg bg-gray-50 px-4 py-6 text-center text-sm text-gray-500">
                    No sensors registered in storage zones.
                </p>
            ) : (
                <ul className="mt-4 flex flex-col gap-3">
                    {sensors.map((sensor) => (
                        <li key={sensor.id} className="flex items-center gap-3 rounded-lg border border-gray-100 px-3 py-2">
                            {sensor.kind === "humidity" ? (
                                <Droplets size={16} color="#0284c7" aria-hidden="true" />
                            ) : (
                                <Thermometer size={16} color="#c2630b" aria-hidden="true" />
                            )}
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-xs font-semibold text-gray-800">{sensor.label}</p>
                                <span
                                    className="mt-0.5 inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold"
                                    style={
                                        sensor.online
                                            ? { background: "#dcfce7", color: "#166534" }
                                            : { background: "#fee2e2", color: "#b91c1c" }
                                    }
                                >
                                    {sensor.online ? "Online" : "No recent data"}
                                </span>
                            </div>
                            <span className="text-sm font-bold text-gray-900">{sensor.valueText}</span>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

export { AlertTriangle };