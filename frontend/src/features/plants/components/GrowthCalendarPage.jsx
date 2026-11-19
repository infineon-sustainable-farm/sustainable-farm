import { useEffect, useState } from "react";
import { Info, Loader2 } from "lucide-react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Card, EmptyState, ErrorState, PageHeader } from "../../../shared/ui";
import { useGrowthCalendar, useGrowthCalendarBlocks } from "../hooks/useGrowthCalendar";
import BlockFilter from "./BlockFilter";
import GrowthCalendarTable from "./GrowthCalendarTable";

/**
 * Explains, next to the table, why age and phase show a dash. Only rendered
 * when a displayed row actually lacks its planting date, and the Zalka wording
 * is only quoted for rows that come from that study.
 */
function MissingPlantingDateNote({ entries }) {
    const { t } = useTranslation("plants");
    const undated = entries.filter((entry) => !entry.plantingDate);
    if (undated.length === 0) return null;
    const fromZalka = undated.some((entry) => entry.source === "Zalka_2025");

    return (
        <Card className="flex gap-3 text-sm text-text-muted">
            <Info size={18} aria-hidden="true" className="mt-0.5 shrink-0 text-info-fg" />
            <p>{fromZalka ? t("calendar.missingDateZalka") : t("calendar.missingDate")}</p>
        </Card>
    );
}

/**
 * Says what comes from where. The age bands of the phases are the reference
 * study's, in every profile; the study puts year 5 in two phases and stops at
 * year 7, and the legend says how both are read. The share of the full yield
 * of each phase has sources of its own, shown with it on Harvest & Maturity.
 * The bands and the phase names both come from the API, and nothing here
 * computes or decides a threshold.
 */
function PhaseLegend() {
    return (
        <p className="px-1 text-xs text-text-muted">
            <Trans
                ns="plants"
                i18nKey="calendar.legend"
                components={{
                    page: (
                        <Link
                            to="/plants/harvest-maturity"
                            className="font-semibold text-text-accent underline underline-offset-2 hover:no-underline"
                        />
                    ),
                }}
            />
        </p>
    );
}

export default function GrowthCalendarPage() {
    const { t } = useTranslation("plants");
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");

    useEffect(() => {
        document.title = t("calendar.documentTitle");
    }, [t]);

    const { data: entries, isPending, isError, refetch, isFetching } = useGrowthCalendar({
        blockCode: selectedBlock || null,
    });
    const { data: blocks } = useGrowthCalendarBlocks();

    return (
        <div className="p-6 space-y-6">
            <PageHeader title={t("calendar.title")} subtitle={t("calendar.subtitle")} />

            <BlockFilter
                blocks={blocks ?? []}
                value={selectedBlock}
                onChange={setSelectedBlock}
                disabled={isPending}
            />

            {isPending && (
                <Card className="flex items-center justify-center gap-3 py-16 text-sm text-text-muted">
                    <Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
                    <span role="status">{t("calendar.loading")}</span>
                </Card>
            )}

            {isError && (
                <Card>
                    <ErrorState
                        message={`${t("calendar.loadError")} ${t("calendar.loadErrorHint")}`}
                        onRetry={() => refetch()}
                        retrying={isFetching}
                        retryLabel={t("common.retry")}
                    />
                </Card>
            )}

            {!isPending && !isError && entries.length === 0 && (
                <Card>
                    {selectedBlock ? (
                        <EmptyState title={t("calendar.emptyFilteredTitle")} message={t("calendar.emptyFilteredHint")} />
                    ) : (
                        <EmptyState title={t("calendar.emptyTitle")} message={t("calendar.emptyHint")} />
                    )}
                </Card>
            )}

            {!isPending && !isError && entries.length > 0 && (
                <>
                    <GrowthCalendarTable entries={entries} />
                    <PhaseLegend />
                    <MissingPlantingDateNote entries={entries} />
                </>
            )}
        </div>
    );
}
