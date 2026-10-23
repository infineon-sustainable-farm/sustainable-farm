/*
 * Same colours as the low ratings of VigorBadge, which stays untouched: the
 * border is the shared --color-error token, and the label is its darkened hue
 * on its lightened hue.
 *   #9a3f32 on #f7e9e7 -> 5.67:1
 */
const LOW_STOCK_STYLE = "border-error bg-[#f7e9e7] text-[#9a3f32]";

/**
 * Stock status of a fertilizer, as decided by the API: "Low stock" when the
 * stock is at or below the alert threshold, nothing otherwise. No stock or
 * threshold is compared here.
 */
export default function StockStatusBadge({ belowThreshold }) {
    if (!belowThreshold) return null;
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${LOW_STOCK_STYLE}`}>
            Low stock
        </span>
    );
}
