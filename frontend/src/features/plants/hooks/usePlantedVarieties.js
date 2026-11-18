import { useTranslation } from "react-i18next";
import { useVarieties } from "./useVarieties";

/** Today as "YYYY-MM-DD", in local time: the latest date the harvest forms accept. */
export function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * The varieties with a recorded planting on a block, and the note shown under
 * the variety list when there is none to offer: the API refuses a harvest or
 * an observation of any other.
 */
export function usePlantedVarieties(blockCode) {
    const { t } = useTranslation("plants");
    const varieties = useVarieties({ blockCode: blockCode || null });
    // A variety row has a current tree count once a planting is recorded for it.
    const plantedVarieties = blockCode && varieties.data
        ? varieties.data.filter((variety) => variety.currentTreeCount !== null && variety.currentTreeCount !== undefined)
        : [];

    let varietyNote = null;
    if (blockCode && varieties.isError) {
        varietyNote = t("harvest.form.varietiesError");
    } else if (blockCode && varieties.isSuccess && plantedVarieties.length === 0) {
        varietyNote = t("harvest.form.noPlanted");
    }
    return { plantedVarieties, varietyNote };
}
