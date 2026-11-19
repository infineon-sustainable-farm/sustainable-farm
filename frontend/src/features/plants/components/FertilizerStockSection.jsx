import { useTranslation } from "react-i18next";
import { Card, EmptyState } from "../../../shared/ui";
import { useFertilizers } from "../hooks/useFertilizers";
import FertilizerStockTable from "./FertilizerStockTable";
import { SectionLoadError, SectionLoading } from "./FertilizerSectionStatus";

/**
 * Current stock of every fertilizer of the catalogue. The stock and the
 * low-stock status come computed by the API.
 *
 * `addAction` is the page's "Add a fertilizer" button, offered again when the
 * catalogue is empty.
 */
export default function FertilizerStockSection({ addAction }) {
    const { t } = useTranslation("plants");
    const query = useFertilizers();
    const { data: fertilizers, isPending, isError } = query;

    return (
        <section aria-labelledby="current-stock-title" className="flex flex-col gap-4">
            <h2 id="current-stock-title" className="text-lg font-semibold">
                {t("fertilizer.stock.title")}
            </h2>

            {isPending && <SectionLoading>{t("fertilizer.stock.loading")}</SectionLoading>}

            {isError && <SectionLoadError title={t("fertilizer.stock.loadError")} query={query} />}

            {!isPending && !isError && fertilizers.length === 0 && (
                <Card>
                    <EmptyState className="py-8" message={t("fertilizer.stock.empty")} action={addAction} />
                </Card>
            )}

            {!isPending && !isError && fertilizers.length > 0 && <FertilizerStockTable fertilizers={fertilizers} />}
        </section>
    );
}
