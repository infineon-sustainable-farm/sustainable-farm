import { useTranslation } from "react-i18next";
import { ErrorState, Skeleton } from "../../../shared/ui";

/**
 * Skeleton rows shown while a list of the module loads. The loading text ("Loading staff…") is
 * announced to screen readers.
 */
export function LoadingBlock({ label, rows = 4 }) {
    const { t } = useTranslation("common");
    return (
        <div className="flex flex-col gap-2" aria-busy="true">
            <p role="status" className="sr-only">
                {label ?? t("status.loading")}
            </p>
            {Array.from({ length: rows }, (_, index) => (
                <Skeleton key={index} className="h-10 w-full" />
            ))}
        </div>
    );
}

/**
 * Load failure of a screen or list of the module: the translated title ("Staff could not be
 * loaded") followed by the request failure, and the kit Retry button.
 */
export function LoadError({ title, onRetry, retrying = false, className }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <ErrorState
            className={className}
            message={t("errors.loadFailed", { title })}
            onRetry={onRetry}
            retrying={retrying}
        />
    );
}
