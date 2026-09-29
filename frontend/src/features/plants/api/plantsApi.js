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

/**
 * Records a planting: date, block, variety and number of trees.
 *
 * The values go out as entered. The API trims and upper-cases the block code
 * (" a " becomes "A"), answers 400 with fieldErrors for an invalid value, and
 * 409 when the variety is already planted on that block.
 */
export function createPlanting(body) {
    return apiClient.post(PLANTS_ENDPOINTS.PLANTINGS, body);
}
