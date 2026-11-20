import { useQuery } from "@tanstack/react-query";
import { fetchFarmFields } from "../api/plantsApi";

/** Shared with every block picker of plants: one request for the fields of the referential. */
const FARM_FIELDS_QUERY_KEY = ["farm", "fields"];

/**
 * The blocks plants can write on: the fields of the farm referential that carry a block code,
 * sorted by code, restricted to one farm when `farmId` is given. A field without a code is not
 * a plants block yet: the Farms & fields screen gives it one.
 */
export function useFarmBlocks({ farmId = null } = {}) {
    return useQuery({
        queryKey: FARM_FIELDS_QUERY_KEY,
        queryFn: fetchFarmFields,
        select: (fields) =>
            (fields ?? [])
                .filter((field) => field.code)
                .filter((field) => farmId === null || farmId === undefined || farmId === "" || String(field.farmId) === String(farmId))
                .sort((a, b) => a.code.localeCompare(b.code)),
    });
}
