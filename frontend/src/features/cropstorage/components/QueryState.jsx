import { useTranslation } from "react-i18next";
import { ErrorState, Skeleton } from "../../../shared/ui";

/**
 * Loading and error states of one query, with the kit: skeleton blocks while it loads, an
 * error with a retry button when it failed. Renders nothing once the data is there.
 */
export default function QueryState({ query, errorKey, rows = 3 }) {
    const { t } = useTranslation("cropstorage");
    if (query.isPending) {
        return (
            <div className="flex flex-col gap-2" aria-busy="true">
                {Array.from({ length: rows }, (_, index) => (
                    <Skeleton key={index} className="h-10 w-full" />
                ))}
            </div>
        );
    }
    if (query.isError) {
        return (
            <ErrorState
                message={t(errorKey, { message: query.error?.message ?? "" })}
                onRetry={() => query.refetch()}
                retrying={query.isFetching}
            />
        );
    }
    return null;
}
