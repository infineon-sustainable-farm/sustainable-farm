import { useTranslation } from "react-i18next";
import { Card, EmptyState } from "../../../shared/ui";
import { useTreatments } from "../hooks/useTreatments";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";
import TreatmentsTable from "./TreatmentsTable";

/**
 * The treatment record of the selected block: the treatments answering a
 * finding and the preventive ones, by date, with the date from which the
 * block can be harvested again, computed by the API.
 *
 * `recordAction` is the page's "Record a preventive treatment" button,
 * offered again when no treatment is recorded at all.
 */
export default function TreatmentsSection({ blockCode, recordAction }) {
    const { t } = useTranslation("plants");
    const query = useTreatments({ blockCode });
    const { data: treatments, isPending, isError } = query;

    return (
        <Card
            aria-labelledby="treatment-record-title"
            title={<span id="treatment-record-title">{t("health.treatments.title")}</span>}
            subtitle={t("health.treatments.subtitle")}
        >
            {isPending && <HealthSectionLoading>{t("health.treatments.loading")}</HealthSectionLoading>}

            {isError && (
                <HealthSectionError
                    title={t("health.treatments.loadError")}
                    detail={t("common.checkConnection")}
                    query={query}
                />
            )}

            {!isPending && !isError && treatments.length === 0 && (
                blockCode ? (
                    <EmptyState
                        className="py-8"
                        title={t("health.treatments.emptyForBlock")}
                        message={t("health.treatments.emptyForBlockHint")}
                    />
                ) : (
                    <EmptyState
                        className="py-8"
                        title={t("health.treatments.emptyTitle")}
                        message={t("health.treatments.emptyHint")}
                        action={recordAction}
                    />
                )
            )}

            {!isPending && !isError && treatments.length > 0 && <TreatmentsTable treatments={treatments} />}
        </Card>
    );
}
