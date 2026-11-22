import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Bell, Droplets, Gauge, Radio, Thermometer } from "lucide-react";
import { Card, EmptyState, StatusBadge } from "../../../shared/ui";
import { GROUP, enumLabel } from "../utils/enums";
import {
    CAPACITY_BAR_CLASSES,
    CAPACITY_COLORS,
    EMPTY,
    capacityLevel,
    formatDateTime,
    formatNumber,
    formatPercentage,
    formatQuantity,
} from "../utils/formatters";
import { findMatchingBatches, getBatchDisplayId, getBatchProductLabel } from "../utils/batchSearch";

function getNonNegativeNumber(value) {
    if (value == null || (typeof value === "string" && value.trim() === "")) return null;
    const number = Number(value);
    return Number.isFinite(number) && number >= 0 ? number : null;
}

export function StorageGauge({ percentage, usedKg, capacityKg, zones = [] }) {
    const { t } = useTranslation("cropstorage");
    const rawPercentage =
        percentage == null || (typeof percentage === "string" && percentage.trim() === "") ? Number.NaN : Number(percentage);
    const numericUsed = getNonNegativeNumber(usedKg);
    const numericCapacity = getNonNegativeNumber(capacityKg);
    const hasUsed = numericUsed != null;
    const hasCapacity = numericCapacity != null && numericCapacity > 0;
    const displayPercentage = Number.isFinite(rawPercentage) && rawPercentage >= 0 && hasUsed && hasCapacity ? rawPercentage : null;
    const clampedPercentage = displayPercentage == null ? 0 : Math.min(Math.max(displayPercentage, 0), 100);
    const [shown, setShown] = useState(0);

    useEffect(() => {
        const id = requestAnimationFrame(() => setShown(clampedPercentage));
        return () => cancelAnimationFrame(id);
    }, [clampedPercentage]);

    const arcColor = CAPACITY_COLORS[capacityLevel(displayPercentage ?? Number.NaN)];
    const percentageText = displayPercentage == null ? EMPTY : t("percent", { value: formatPercentage(displayPercentage) });
    const capacityText =
        hasUsed || hasCapacity
            ? `${hasUsed ? formatQuantity(numericUsed) : EMPTY} / ${hasCapacity ? formatQuantity(numericCapacity) : EMPTY}`
            : t("overview.gauge.unavailable");

    return (
        <Card
            title={
                <span className="flex items-center gap-2">
                    <Gauge size={18} aria-hidden="true" className="text-text-accent" />
                    {t("overview.gauge.title")}
                </span>
            }
        >
            {!zones || zones.length === 0 ? (
                <EmptyState message={t("overview.gauge.noZone")} />
            ) : (
                <>
                    <div className="relative mx-auto max-w-72">
                        <svg
                            viewBox="0 0 220 130"
                            role="img"
                            aria-label={
                                displayPercentage == null
                                    ? t("overview.gauge.ariaUnavailable")
                                    : t("overview.gauge.aria", { value: formatPercentage(displayPercentage) })
                            }
                            className="block h-40 w-full"
                        >
                            <path
                                d="M 30 100 A 80 80 0 0 1 190 100"
                                fill="none"
                                stroke="var(--color-neutral-bg)"
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
                                className="transition-[stroke-dashoffset] duration-(--duration-slow) ease-standard motion-reduce:transition-none"
                            />
                        </svg>
                        <div className="absolute inset-x-0 bottom-0 px-2 text-center">
                            <p className="whitespace-nowrap text-3xl leading-none font-bold tracking-tight text-text">{percentageText}</p>
                            <p className="mt-1 whitespace-nowrap text-sm font-medium text-text-muted">{capacityText}</p>
                        </div>
                    </div>
                    <div className="mt-3 flex flex-col gap-3">
                        {zones.map((zone) => {
                            const capacity = getNonNegativeNumber(zone.capacityKg);
                            const used = getNonNegativeNumber(zone.usedKg ?? zone.currentQuantityKg);
                            const zonePercent = capacity != null && capacity > 0 && used != null ? (used / capacity) * 100 : null;
                            const bounded = zonePercent == null ? 0 : Math.min(zonePercent, 100);
                            const label = zone.name ?? zone.zoneCode;
                            return (
                                <div key={zone.id ?? zone.zoneCode} className="flex items-center gap-3">
                                    <span className="w-24 truncate text-xs font-medium text-text-muted">{label}</span>
                                    <div
                                        className="h-2 flex-1 overflow-hidden rounded-pill bg-neutral-bg"
                                        role="progressbar"
                                        aria-label={t("overview.gauge.zoneAria", { zone: label })}
                                        aria-valuemin={0}
                                        aria-valuemax={100}
                                        aria-valuenow={zonePercent == null ? undefined : bounded}
                                        aria-valuetext={
                                            zonePercent == null
                                                ? t("zones.usageUnavailable")
                                                : t("zones.usedValue", { value: formatPercentage(zonePercent) })
                                        }
                                    >
                                        <div
                                            className={`h-full rounded-pill ${CAPACITY_BAR_CLASSES[capacityLevel(zonePercent ?? Number.NaN)]}`}
                                            style={{ width: `${bounded}%` }}
                                        />
                                    </div>
                                    <span className="w-14 text-right text-xs tabular-nums text-text-muted">
                                        {zonePercent == null ? EMPTY : t("percent", { value: formatPercentage(zonePercent) })}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                </>
            )}
        </Card>
    );
}

export function TrendChart({ title, readings = [], field, unit, color, targetMin, targetMax, emptyMessage }) {
    const { t } = useTranslation("cropstorage");
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
        path = values.map((v, i) => `${i === 0 ? "M" : "L"} ${i * stepX} ${toY(v).toFixed(1)}`).join(" ");
        area = `${path} L ${((values.length - 1) * stepX).toFixed(1)} ${height} L 0 ${height} Z`;
        if (targetMax != null) targetY = toY(targetMax);
    }

    return (
        <Card title={title} actions={<span className="text-xs text-text-muted">{t("overview.trend.last24h")}</span>}>
            {values.length === 0 ? (
                <EmptyState message={emptyMessage} />
            ) : (
                <>
                    <p className="text-2xl font-bold text-text">
                        {formatNumber(latest)}
                        <span className="ml-1 text-sm font-medium text-text-muted">{unit}</span>
                    </p>
                    <svg viewBox={`0 0 ${width} ${height}`} className="mt-2 h-32 w-full" preserveAspectRatio="none" aria-hidden="true">
                        <path d={area} fill={color} fillOpacity="0.15" stroke="none" />
                        {targetY != null && (
                            <line x1="0" y1={targetY} x2={width} y2={targetY} stroke={color} strokeWidth="1" strokeDasharray="4 3" opacity="0.7" />
                        )}
                        <path d={path} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    <div className="mt-1 flex justify-between text-xs text-text-muted">
                        <span>{t("overview.trend.ago")}</span>
                        <span>{t("overview.trend.now")}</span>
                        {targetMax != null && <span>{t("overview.trend.target", { min: targetMin, max: targetMax, unit })}</span>}
                    </div>
                </>
            )}
        </Card>
    );
}

export function ActiveAlertPanel({ alerts = [], to }) {
    const { t } = useTranslation("cropstorage");
    const first = alerts[0] ?? null;
    return (
        <Card
            title={
                <span className="flex items-center gap-2">
                    <Bell size={18} aria-hidden="true" className={first ? "text-error-fg" : "text-success-fg"} />
                    {t("overview.alert.title")}
                </span>
            }
        >
            {!first ? (
                <div className="rounded-tile bg-success-bg px-4 py-6 text-center">
                    <p className="text-sm font-semibold text-success-fg">{t("overview.alert.none")}</p>
                    <p className="mt-1 text-xs text-text-muted">{t("overview.alert.monitored")}</p>
                </div>
            ) : (
                <div className="rounded-tile bg-error-tint px-4 py-4">
                    <p className="text-sm font-bold text-error-fg">
                        {t("overview.alert.detected", { type: enumLabel(t, GROUP.alertType, first.type) })}
                    </p>
                    <p className="mt-1 text-xs text-text">{first.message ?? t("overview.alert.defaultMessage")}</p>
                    <p className="mt-2 text-xs text-text-muted">{formatDateTime(first.createdAt)}</p>
                    <Link
                        to={to}
                        className="mt-3 inline-block rounded-control bg-danger px-3 py-1.5 text-xs font-semibold text-on-primary hover:bg-danger-pressed"
                    >
                        {t("overview.alert.details")}
                    </Link>
                </div>
            )}
        </Card>
    );
}

export function BatchesMiniTable({ batches = [], zoneClimates = {}, zoneKeyOf, search = "", viewAllTo, tableRef }) {
    const { t } = useTranslation("cropstorage");
    const rows = findMatchingBatches(batches, search, t).slice(0, 5);

    return (
        <section ref={tableRef} className="min-w-0 rounded-card border border-border-subtle bg-surface-raised p-5 shadow-raised lg:col-span-2">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-text">{t("overview.batches.title")}</h2>
                <Link to={viewAllTo} className="text-xs font-semibold text-text-accent hover:underline">
                    {t("overview.batches.viewAll")}
                </Link>
            </div>
            <div className="mt-3 overflow-x-auto">
                <table className="w-full text-left text-xs">
                    <thead>
                        <tr className="border-b border-border-divider text-text-muted uppercase">
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.id")}</th>
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.product")}</th>
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.quantity")}</th>
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.temperature")}</th>
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.humidity")}</th>
                            <th className="py-2 pr-3 font-semibold">{t("overview.batches.columns.status")}</th>
                            <th className="py-2 font-semibold">{t("overview.batches.columns.updated")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.length === 0 ? (
                            <tr>
                                <td colSpan="7" className="py-6 text-center text-text-muted">
                                    {t("overview.batches.noMatch")}
                                </td>
                            </tr>
                        ) : (
                            rows.map((batch) => {
                                const key = zoneKeyOf(batch);
                                const climate = key == null ? null : zoneClimates[key];
                                return (
                                    <tr key={batch.id ?? batch.batchCode} className="border-b border-border-subtle">
                                        <td className="py-2 pr-3 font-semibold text-text">{getBatchDisplayId(batch)}</td>
                                        <td className="py-2 pr-3 text-text-muted">{getBatchProductLabel(batch, t)}</td>
                                        <td className="py-2 pr-3 text-text-muted">{formatQuantity(batch.currentQuantityKg)}</td>
                                        <td className="py-2 pr-3 text-text-muted">
                                            {climate?.temperature == null ? EMPTY : t("units.celsius", { value: formatNumber(climate.temperature) })}
                                        </td>
                                        <td className="py-2 pr-3 text-text-muted">
                                            {climate?.humidity == null ? EMPTY : t("percent", { value: formatNumber(climate.humidity) })}
                                        </td>
                                        <td className="py-2 pr-3">
                                            {batch.status ? (
                                                <StatusBadge
                                                    tone={batch.status === "STORED" ? "success" : "neutral"}
                                                    label={enumLabel(t, GROUP.batchStatus, batch.status)}
                                                />
                                            ) : (
                                                EMPTY
                                            )}
                                        </td>
                                        <td className="py-2 text-text-muted">{formatDateTime(batch.updatedAt)}</td>
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

export function SensorsPanel({ sensors = [] }) {
    const { t } = useTranslation("cropstorage");
    return (
        <Card
            title={
                <span className="flex items-center gap-2">
                    <Radio size={18} aria-hidden="true" className="text-text-accent" />
                    {t("overview.sensors.title")}
                </span>
            }
        >
            {sensors.length === 0 ? (
                <EmptyState message={t("overview.sensors.none")} />
            ) : (
                <ul className="flex flex-col gap-3">
                    {sensors.map((sensor) => (
                        <li key={sensor.id} className="flex items-center gap-3 rounded-tile border border-border-subtle px-3 py-2">
                            {sensor.kind === "humidity" ? (
                                <Droplets size={16} aria-hidden="true" className="text-info-fg" />
                            ) : (
                                <Thermometer size={16} aria-hidden="true" className="text-warning-fg" />
                            )}
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-xs font-semibold text-text">{sensor.label}</p>
                                <StatusBadge
                                    tone={sensor.online ? "success" : "error"}
                                    label={t(sensor.online ? "overview.sensors.online" : "overview.sensors.noData")}
                                />
                            </div>
                            <span className="text-sm font-bold text-text">{sensor.valueText}</span>
                        </li>
                    ))}
                </ul>
            )}
        </Card>
    );
}
