import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, Input } from "../../../shared/ui";
import { useHarvests } from "../hooks/useHarvests";
import { formatDate } from "../utils/format";
import { SectionError, SectionLoading } from "./HarvestSectionStates";
import HarvestsTable from "./HarvestsTable";

/**
 * One date filter of the list. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from the forecast's "From".
 * The chosen date is written out under the field in letters, as the browser
 * shows its own day/month order.
 */
function DateFilter({ id, label, value, onChange }) {
    return (
        <div className="flex items-start gap-3">
            <label id={`${id}-label`} htmlFor={id} className="pt-2 text-sm font-medium">
                {label}
            </label>
            <div className="flex flex-col gap-1">
                <Input
                    id={id}
                    type="date"
                    value={value}
                    onChange={(event) => onChange(event.target.value)}
                    aria-labelledby={`recorded-harvests-title ${id}-label`}
                    aria-describedby={value ? `${id}-chosen` : undefined}
                />
                {value && (
                    <p id={`${id}-chosen`} className="text-xs text-text-muted">
                        {formatDate(value)}
                    </p>
                )}
            </div>
        </div>
    );
}

/**
 * Recorded harvests for the selected block, between two optional dates, both
 * included. Filters the API cannot match simply give an empty list.
 *
 * `recordAction` is the page's "Record a harvest" button, offered again when
 * no harvest is recorded at all.
 */
export default function HarvestsSection({ blockCode, recordAction }) {
    const { t } = useTranslation("plants");
    // "YYYY-MM-DD" as the date fields give it; an empty string means "no bound".
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const query = useHarvests({
        blockCode,
        from: from || null,
        to: to || null,
    });
    const { data: harvests, isPending, isError } = query;
    const isFiltered = Boolean(blockCode || from || to);

    return (
        <section aria-labelledby="recorded-harvests-title" className="flex flex-col gap-4">
            <h2 id="recorded-harvests-title" className="text-lg font-semibold">
                {t("harvest.harvests.title")}
            </h2>

            <Card>
                <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
                    <DateFilter id="recorded-harvests-from" label={t("harvest.harvests.from")} value={from} onChange={setFrom} />
                    <DateFilter id="recorded-harvests-to" label={t("harvest.harvests.to")} value={to} onChange={setTo} />
                </div>
            </Card>

            {isPending && <SectionLoading label={t("harvest.harvests.loading")} />}

            {isError && <SectionError title={t("harvest.harvests.error")} query={query} />}

            {!isPending && !isError && harvests.length === 0 && (
                <Card>
                    {isFiltered ? (
                        <EmptyState
                            title={t("harvest.harvests.filteredTitle")}
                            message={t("harvest.harvests.filteredHint")}
                        />
                    ) : (
                        <EmptyState message={t("harvest.harvests.emptyTitle")} action={recordAction} />
                    )}
                </Card>
            )}

            {!isPending && !isError && harvests.length > 0 && <HarvestsTable harvests={harvests} />}
        </section>
    );
}
