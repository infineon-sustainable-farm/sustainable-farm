import { useTranslation } from "react-i18next";
import { Button, Field, Input, Select } from "../../../shared/ui";
import { useLabels } from "../utils/labels";

const BOOKING_STATUSES = ["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"];

/**
 * Filters of the bookings list: status, activity, date, and the cancelled toggle. The API applies
 * only the first non-null filter, so the page clears the others when one changes.
 */
export default function BookingFilters({
    filters,
    activities,
    onFilterChange,
    onClear,
    showCancelled,
    onShowCancelledChange,
}) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const hasActiveFilter = Boolean(filters.status || filters.activityId || filters.date);

    return (
        <div className="mb-4 flex flex-wrap items-end gap-3">
            <Field id="bk-filter-status" className="min-w-40" label={t("booking.filters.status")}>
                <Select value={filters.status} onChange={(event) => onFilterChange("status", event.target.value)}>
                    <option value="">{t("booking.filters.allStatuses")}</option>
                    {BOOKING_STATUSES.map((status) => (
                        <option key={status} value={status}>
                            {labels.enum(status)}
                        </option>
                    ))}
                </Select>
            </Field>

            <Field id="bk-filter-activity" className="min-w-48" label={t("booking.filters.activity")}>
                <Select value={filters.activityId} onChange={(event) => onFilterChange("activityId", event.target.value)}>
                    <option value="">{t("booking.filters.allActivities")}</option>
                    {activities.map((activity) => (
                        <option key={activity.id} value={activity.id}>
                            {activity.name}
                        </option>
                    ))}
                </Select>
            </Field>

            <Field id="bk-filter-date" label={t("booking.filters.date")}>
                <Input type="date" value={filters.date} onChange={(event) => onFilterChange("date", event.target.value)} />
            </Field>

            {hasActiveFilter && (
                <Button variant="secondary" onClick={onClear}>
                    {t("booking.filters.clear")}
                </Button>
            )}

            <label className="flex items-center gap-2 pb-2.5 text-sm text-text">
                <input
                    type="checkbox"
                    checked={showCancelled}
                    onChange={(event) => onShowCancelledChange(event.target.checked)}
                    className="h-4 w-4 accent-primary"
                />
                {t("booking.filters.showCancelled")}
            </label>
        </div>
    );
}
