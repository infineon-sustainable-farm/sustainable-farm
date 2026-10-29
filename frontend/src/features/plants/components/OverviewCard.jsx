import { Loader2, TriangleAlert } from "lucide-react";

/**
 * Frame of one chart of the Overview: its title and caption, then its loading
 * state, its error with a Retry, or its content. Each chart reads its own
 * request, so one failing request leaves the other charts in place.
 *
 * `query` is the TanStack Query result the chart reads; `children` is only
 * rendered once its data is there.
 */
export default function OverviewCard({ titleId, title, caption, query, loadingText, errorText, children, className = "" }) {
    const { isPending, isError, refetch, isFetching } = query;

    return (
        <section
            aria-labelledby={titleId}
            className={`flex min-w-0 flex-col gap-4 rounded-xl border border-gray-200 bg-white p-5 ${className}`}
        >
            <div>
                <h3 id={titleId} className="font-heading text-base font-bold text-gray-900">
                    {title}
                </h3>
                {caption && <p className="mt-1 text-sm text-gray-600">{caption}</p>}
            </div>

            {isPending && (
                <div className="flex items-center gap-3 py-6 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    {loadingText}
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-start gap-3 rounded-lg border border-error/30 bg-error/5 px-4 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">{errorText}</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && children}
        </section>
    );
}
