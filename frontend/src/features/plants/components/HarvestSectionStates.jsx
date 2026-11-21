import { Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, ErrorState } from "../../../shared/ui";

/** A section of the Harvest & Maturity screen still loading: a spinner and what loads, announced. */
export function SectionLoading({ label }) {
    return (
        <Card>
            <p role="status" className="flex items-center gap-3 text-sm text-text-muted">
                <Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
                {label}
            </p>
        </Card>
    );
}

/** A section whose request failed: what is unavailable, what to do, and a Retry. */
export function SectionError({ title, query }) {
    const { t } = useTranslation("plants");
    return (
        <Card>
            <ErrorState
                className="py-6"
                message={`${title} ${t("common.checkConnection")}`}
                onRetry={() => query.refetch()}
                retrying={query.isFetching}
                retryLabel={t("common.retry")}
            />
        </Card>
    );
}
