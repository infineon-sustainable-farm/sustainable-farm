import { apiClient } from "../../../shared/api/client";
import { PLANTS_ENDPOINTS } from "./endpoints";

/**
 * Fetches the varieties, optionally filtered.
 *
 * Filters are sent as raw domain values: the block goes out as "A", not as the
 * "Block A" label used on screen. An omitted or empty filter is left out of the
 * query string entirely so the API returns every row.
 */
export function fetchVarieties({ blockCode, farmId } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;

    return apiClient.get(PLANTS_ENDPOINTS.VARIETIES, { params });
}

/**
 * Fetches a single variety by its identifier.
 */
export function fetchVarietyById(id) {
    return apiClient.get(`${PLANTS_ENDPOINTS.VARIETIES}/${id}`);
}

/**
 * Fetches the growth calendar entries, optionally filtered.
 *
 * Same filter rules as fetchVarieties: raw block value, empty filters omitted.
 * Tree age and growth phase come computed by the API; they are never computed
 * or guessed here.
 */
export function fetchGrowthCalendar({ blockCode, farmId } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;

    return apiClient.get(PLANTS_ENDPOINTS.GROWTH_CALENDAR, { params });
}
