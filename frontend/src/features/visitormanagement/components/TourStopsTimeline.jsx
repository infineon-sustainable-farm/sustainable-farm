import { useState } from "react";
import { Ban, CircleDot, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, ConfirmDialog, StatusBadge } from "../../../shared/ui";
import { useVisitorFormat } from "../utils/format";

/**
 * The vertical timeline of the standard tour: each stop carries edit and deactivate actions.
 * Deactivation has no counterpart in the API (no active flag on the payload, no reactivation
 * call), so it asks for confirmation and the stop leaves the timeline for good. Stops come from
 * the API already ordered by position.
 */
export default function TourStopsTimeline({
    stops,
    pendingDeactivateId,
    deactivateError,
    onEdit,
    onDeactivate,
}) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const [confirmTarget, setConfirmTarget] = useState(null);

    if (stops.length === 0) {
        return <p className="text-sm text-text-muted">{t("education.stops.empty")}</p>;
    }

    function meta(stop) {
        const parts = [t("education.minutes", { value: stop.durationMinutes })];
        if (stop.location) parts.push(stop.location);
        if (stop.maxCapacity) {
            parts.push(t("education.stops.maxPeople", { value: format.number(stop.maxCapacity) }));
        }
        return parts.join(" · ");
    }

    return (
        <>
            <ol className="flex flex-col gap-4 border-l-2 border-primary pl-4">
                {stops.map((stop) => {
                    const isPending = pendingDeactivateId === stop.id;
                    return (
                        <li key={stop.id} className={stop.active ? "" : "opacity-60"}>
                            <p className="flex items-center gap-2 text-base font-semibold text-text">
                                <CircleDot size={16} aria-hidden="true" className="shrink-0 text-primary" />
                                {stop.name}
                                {!stop.active && (
                                    <StatusBadge tone="neutral" label={t("enums.INACTIVE")} />
                                )}
                            </p>
                            <p className="mt-0.5 text-xs text-text-muted">{meta(stop)}</p>
                            {stop.description && <p className="mt-1 text-sm text-text">{stop.description}</p>}
                            {stop.demo && (
                                <p className="mt-1 text-xs text-text-muted">
                                    <span className="font-semibold">{t("education.stops.demoLabel")}</span>{" "}
                                    {stop.demo}
                                </p>
                            )}
                            {stop.safetyNotes && (
                                <p className="mt-1 text-xs text-text-muted">
                                    <span className="font-semibold">{t("education.stops.safetyLabel")}</span>{" "}
                                    {stop.safetyNotes}
                                </p>
                            )}

                            <div className="mt-2 flex flex-wrap gap-2">
                                <Button
                                    size="sm"
                                    variant="secondary"
                                    icon={Pencil}
                                    disabled={isPending}
                                    onClick={() => onEdit(stop)}
                                >
                                    {t("education.actions.edit")}
                                </Button>
                                <Button
                                    size="sm"
                                    variant="secondary"
                                    icon={Ban}
                                    loading={isPending}
                                    onClick={() => setConfirmTarget(stop)}
                                >
                                    {t("education.actions.deactivate")}
                                </Button>
                            </div>
                            {deactivateError?.id === stop.id && (
                                <p role="alert" className="mt-1 text-xs text-error-fg">
                                    {deactivateError.message}
                                </p>
                            )}
                        </li>
                    );
                })}
            </ol>

            <ConfirmDialog
                open={Boolean(confirmTarget)}
                title={t("education.stops.deactivateTitle")}
                message={
                    confirmTarget
                        ? t("education.stops.deactivateMessage", { name: confirmTarget.name })
                        : ""
                }
                confirmLabel={t("education.actions.yes")}
                cancelLabel={t("education.actions.no")}
                onClose={() => setConfirmTarget(null)}
                onConfirm={() => {
                    onDeactivate(confirmTarget);
                    setConfirmTarget(null);
                }}
            />
        </>
    );
}
