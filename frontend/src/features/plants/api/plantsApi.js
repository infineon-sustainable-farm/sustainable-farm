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

/**
 * Fetches the recorded harvests, optionally filtered.
 *
 * Same filter rules as fetchVarieties: raw block value, empty filters omitted.
 * `from` and `to` are "YYYY-MM-DD" dates, both included by the API.
 */
export function fetchHarvests({ blockCode, farmId, from, to } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;
    if (from) params.from = from;
    if (to) params.to = to;

    return apiClient.get(PLANTS_ENDPOINTS.HARVESTS, { params });
}

/**
 * Records a harvest: date, block, variety and quantity in kilograms.
 *
 * The API answers 400 with fieldErrors for an invalid value, and 422 when the
 * variety has no planting on that block or the harvest predates it.
 */
export function createHarvest(body) {
    return apiClient.post(PLANTS_ENDPOINTS.HARVESTS, body);
}

/**
 * Fetches the expected yield, month by month.
 *
 * `from` ("YYYY-MM") is left out when empty, so the API starts the forecast at
 * its own current month. Every figure comes computed by the API; nothing of the
 * forecast is computed here.
 */
export function fetchYieldForecast({ blockCode, farmId, from, months } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;
    if (from) params.from = from;
    if (months !== null && months !== undefined) params.months = months;

    return apiClient.get(PLANTS_ENDPOINTS.YIELD_FORECAST, { params });
}
