import { Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, ErrorState } from "../../../shared/ui";

/** Loading line of a Fertilizer Inventory section, said to screen readers too. */
export function SectionLoading({ children }) {
    return (
        <Card>
            <p role="status" className="flex items-center justify-center gap-3 py-8 text-sm text-text-muted">
                <Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
                {children}
            </p>
        </Card>
    );
}

/** Load failure of a Fertilizer Inventory section, with its Retry. */
export function SectionLoadError({ title, query }) {
    const { t } = useTranslation("plants");
    return (
        <Card>
            <ErrorState
                className="py-8"
                message={
                    <>
                        <span className="font-semibold">{title}</span>
                        <span className="block text-sm">{t("fertilizer.serverFailed")}</span>
                    </>
                }
                onRetry={() => query.refetch()}
                retrying={query.isFetching}
                retryLabel={t("common.retry")}
            />
        </Card>
    );
}
