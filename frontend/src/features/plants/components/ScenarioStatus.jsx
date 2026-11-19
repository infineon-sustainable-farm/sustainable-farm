import { CircleAlert, Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";

/*
 * Components shared by the sections of the Scenario Analysis screen: the waiting line, the error
 * box with its Retry and the section heading. Their classes are in scenarioStyles.js.
 */

/** A line saying that something is being computed. */
export function LoadingBox({ label, bare = false }) {
    return (
        <div
            role="status"
            className={`flex items-center gap-3 text-sm text-text-muted ${bare ? "py-4" : "rounded-tile border border-border-subtle bg-surface-raised px-5 py-4"}`}
        >
            <Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
            {label}
        </div>
    );
}

/** Retry of a failed request, busy while it runs again. */
export function RetryButton({ onClick, isFetching, size = "md" }) {
    const { t } = useTranslation("plants");
    return (
        <Button size={size} onClick={onClick} disabled={isFetching}>
            {isFetching ? t("common.retrying") : t("common.retry")}
        </Button>
    );
}

/** A failed request: what failed, why, and an action (Retry, or another way out). */
export function ErrorBox({ title, message, action }) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-tile bg-error-tint px-5 py-4">
            <div className="flex items-start gap-3">
                <CircleAlert size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-error-fg" />
                <div>
                    <p className="text-sm font-semibold text-text">{title}</p>
                    <p className="mt-0.5 text-sm text-text">{message}</p>
                </div>
            </div>
            {action}
        </div>
    );
}

/** Title and introduction of a section of the screen. */
export function SectionHeading({ id, title, children }) {
    return (
        <div>
            <h2 id={id} className="text-lg font-semibold break-words text-text">
                {title}
            </h2>
            {children && <p className="mt-1 max-w-3xl text-sm text-text-muted">{children}</p>}
        </div>
    );
}
