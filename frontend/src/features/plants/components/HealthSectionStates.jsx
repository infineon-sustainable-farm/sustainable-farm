import { Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { ErrorState } from "../../../shared/ui";

/** Loading line of a Diseases & Alerts section: a spinner and what is loading. */
export function HealthSectionLoading({ children }) {
    return (
        <p role="status" className="flex items-center justify-center gap-3 py-6 text-sm text-text-muted">
            <Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
            {children}
        </p>
    );
}

/**
 * Failed load of a Diseases & Alerts section: what could not be loaded, why, and a Retry that
 * runs the request again. `query` is the TanStack Query result of the section.
 */
export function HealthSectionError({ title, detail, query }) {
    const { t } = useTranslation("plants");
    return (
        <ErrorState
            className="py-6"
            message={
                <>
                    <span className="font-semibold">{title}</span> {detail}
                </>
            }
            onRetry={() => query.refetch()}
            retrying={query.isFetching}
            retryLabel={t("common.retry")}
        />
    );
}
