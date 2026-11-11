import { TriangleAlert } from "lucide-react";
import { CARD, CARD_CAPTION, CARD_TITLE, ERROR_BOX, ICON_SIZE, PRIMARY_BUTTON, SKELETON } from "./plantsStyles";

/**
 * Frame of one chart of the Overview: its title and caption, then its loading
 * state, its error with a Retry, or its content. Each chart reads its own
 * request, so one failing request leaves the other charts in place.
 *
 * `query` is the TanStack Query result the chart reads; `children` is only
 * rendered once its data is there. While it loads, gray blocks hold the place
 * of the content, and `loadingText` is said to screen readers.
 *
 * The card has exactly two children, its header and its body, so that a row
 * of cards can align their headers through grid subgrid (see OverviewPage).
 */
export default function OverviewCard({ titleId, title, caption, query, loadingText, errorText, children, className = "" }) {
    const { isPending, isError, refetch, isFetching } = query;

    return (
        <section aria-labelledby={titleId} className={`flex min-w-0 flex-col gap-4 ${CARD} ${className}`}>
            <div>
                <h2 id={titleId} className={CARD_TITLE}>
                    {title}
                </h2>
                {caption && <p className={`mt-1 ${CARD_CAPTION}`}>{caption}</p>}
            </div>

            <div className="flex min-w-0 flex-col gap-4">
                {isPending && (
                    <div role="status" className="flex flex-col gap-3">
                        <span className="sr-only">{loadingText}</span>
                        <div aria-hidden="true" className={`h-4 w-1/3 ${SKELETON}`} />
                        <div aria-hidden="true" className={`h-2.5 w-full ${SKELETON}`} />
                        <div aria-hidden="true" className={`h-4 w-2/3 ${SKELETON}`} />
                        <div aria-hidden="true" className={`h-2.5 w-4/5 ${SKELETON}`} />
                    </div>
                )}

                {isError && (
                    <div className={ERROR_BOX}>
                        <div className="flex items-start gap-2">
                            <TriangleAlert size={ICON_SIZE} className="mt-0.5 shrink-0 text-error" />
                            <div>
                                <p className="text-sm font-bold text-on-background">{errorText}</p>
                                <p className="mt-0.5 text-sm text-gray-600">Check your connection, then try again.</p>
                            </div>
                        </div>
                        <button type="button" onClick={() => refetch()} disabled={isFetching} className={PRIMARY_BUTTON}>
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && children}
            </div>
        </section>
    );
}
