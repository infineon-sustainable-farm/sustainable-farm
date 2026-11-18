import { useTranslation } from "react-i18next";
import { formatNumber } from "../utils/format";

/**
 * "3 months", "1 tree": a count with its word, in the active language, for the yield comparison.
 * `word` is months, trees or harvests (keys of plants:varieties.comparison).
 */
export function useCountOf() {
    const { t } = useTranslation("plants");
    return (count, word) => t(`varieties.comparison.${word}`, { count: Number(count), value: formatNumber(count) });
}
