import { useTranslation } from "react-i18next";
import { Card, ErrorState, Skeleton } from "../../../shared/ui";

/**
 * Frame of one chart of the Overview: its title and caption, then its loading
 * state, its error with a Retry, or its content. Each chart reads its own
 * request, so one failing request leaves the other charts in place.
 *
 * `query` is the TanStack Query result the chart reads; `children` is only
 * rendered once its data is there. While it loads, gray blocks hold the place
 * of the content, and `loadingText` is said to screen readers.
 */
export default function OverviewCard({ titleId, title, caption, query, loadingText, errorText, children, className = "" }) {
    const { t } = useTranslation("plants");
    const { isPending, isError, refetch, isFetching } = query;

    return (
        <Card
            aria-labelledby={titleId}
            title={<span id={titleId}>{title}</span>}
            subtitle={caption}
            className={`flex min-w-0 flex-col ${className}`}
        >
            <div className="flex min-w-0 flex-col gap-4">
                {isPending && (
                    <div role="status" className="flex flex-col gap-3">
                        <span className="sr-only">{loadingText}</span>
                        <Skeleton className="h-4 w-1/3" />
                        <Skeleton className="h-2.5 w-full" />
                        <Skeleton className="h-4 w-2/3" />
                        <Skeleton className="h-2.5 w-4/5" />
                    </div>
                )}

                {isError && (
                    <ErrorState
                        className="py-6"
                        message={`${errorText} ${t("common.checkConnection")}`}
                        onRetry={() => refetch()}
                        retrying={isFetching}
                        retryLabel={t("common.retry")}
                    />
                )}

                {!isPending && !isError && children}
            </div>
        </Card>
    );
}
