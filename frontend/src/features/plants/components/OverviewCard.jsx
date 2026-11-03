import { Loader2, TriangleAlert } from "lucide-react";
import { CARD, CARD_CAPTION, CARD_TITLE, ERROR_BOX, ICON_SIZE, PRIMARY_BUTTON } from "./overviewStyles";

/**
 * Frame of one chart of the Overview: its title and caption, then its loading
 * state, its error with a Retry, or its content. Each chart reads its own
 * request, so one failing request leaves the other charts in place.
 *
 * `query` is the TanStack Query result the chart reads; `children` is only
 * rendered once its data is there.
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
                    <div className="flex items-center gap-2 py-4 text-sm text-gray-500">
                        <Loader2 size={ICON_SIZE} className="animate-spin" />
                        {loadingText}
                    </div>
                )}

                {isError && (
                    <div className={ERROR_BOX}>
                        <div className="flex items-start gap-2">
                            <TriangleAlert size={ICON_SIZE} className="mt-0.5 shrink-0 text-error" />
                            <div>
                                <p className="text-sm font-bold text-gray-900">{errorText}</p>
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
