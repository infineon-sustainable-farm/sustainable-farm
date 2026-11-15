import { formatEur, formatXof } from "../utils/format";
import { CELL_NOTE } from "./plantsStyles";

/**
 * A cost at the last purchase price, as the API converts it: the amount in
 * FCFA with the amount in euros under it. While no rate is recorded, the API
 * sends one currency only, and that one is shown alone. While no purchase of
 * the fertilizer has a cost, there is no price: the cell says so instead of
 * showing 0.
 */
export default function PlanCost({ xof, eur }) {
    const hasXof = xof !== null && xof !== undefined;
    const hasEur = eur !== null && eur !== undefined;
    if (!hasXof && !hasEur) {
        return <span className="text-gray-600">No price yet</span>;
    }
    if (!hasXof) return formatEur(eur);
    return (
        <>
            {formatXof(xof)}
            {hasEur && <span className={CELL_NOTE}>{formatEur(eur)}</span>}
        </>
    );
}
