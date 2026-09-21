import { formatNumber } from "../utils/format";

/**
 * The mockup's Summary block: three mini-stats and the 1–5 distribution bars.
 * All figures come from the API summary; the percentages of the distribution
 * are the only thing computed here, from the counts and the total.
 */
export default function FeedbackSummary({ summary }) {
    const total = summary?.total ?? 0;
    const average = summary?.averageRating ?? 0;
    const recommendPct = summary?.recommendPct ?? 0;
    const distribution = summary?.distribution ?? {};

    return (
        <>
            <div className="mb-4.5 grid grid-cols-1 gap-3.5 sm:grid-cols-3">
                <div className="rounded-lg border border-line bg-[#F2FBF9] p-3.5">
                    <p className="mb-1 text-[11px] text-muted">Average rating</p>
                    <p className="font-heading text-[22px] font-bold text-primary-dark">
                        {total > 0 ? `${average.toFixed(1)}/5` : "—"}
                    </p>
                </div>
                <div className="rounded-lg border border-line bg-[#F2FBF9] p-3.5">
                    <p className="mb-1 text-[11px] text-muted">Responses this week</p>
                    <p className="font-heading text-[22px] font-bold text-primary-dark">
                        {formatNumber(total)}
                    </p>
                </div>
                <div className="rounded-lg border border-line bg-[#F2FBF9] p-3.5">
                    <p className="mb-1 text-[11px] text-muted">Would recommend</p>
                    <p className="font-heading text-[22px] font-bold text-primary-dark">
                        {total > 0 ? `${Math.round(recommendPct)}%` : "—"}
                    </p>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                {[5, 4, 3, 2, 1].map((star) => {
                    const count = distribution[star] ?? 0;
                    const percent = total > 0 ? Math.round((count * 100) / total) : 0;
                    return (
                        <div key={star} className="flex items-center gap-2.5 text-xs">
                            <span className="w-11 shrink-0 text-muted">{star}★</span>
                            <div className="h-3 flex-1 overflow-hidden rounded-md bg-[#E8EFED]">
                                <div
                                    className="h-full rounded-md bg-primary"
                                    style={{ width: `${percent}%` }}
                                />
                            </div>
                            <span className="w-9 shrink-0 text-right font-semibold text-primary-dark">
                                {percent}%
                            </span>
                            <span className="w-8 shrink-0 text-right text-muted">
                                {formatNumber(count)}
                            </span>
                        </div>
                    );
                })}
            </div>
        </>
    );
}