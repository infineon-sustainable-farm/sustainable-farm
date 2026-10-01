import { NO_VALUE } from "../utils/format";

/**
 * Renders a formatted value, giving the "no value" dash one single colour
 * across the module.
 *
 * Why it exists: most cells already carry text-gray-700, so their dash picks it
 * up, but two places do not — the Vigor cell has no colour of its own (its badge
 * normally sets it) and the detail modal uses a darker grey for values. Left to
 * inheritance, the same missing value would render black there and gray-800
 * here. Anything other than a dash is passed through untouched.
 */
export default function Value({ children }) {
    return children === NO_VALUE ? <span className="text-gray-700">{NO_VALUE}</span> : children;
}
