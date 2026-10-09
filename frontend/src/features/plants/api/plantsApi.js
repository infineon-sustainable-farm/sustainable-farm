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

/**
 * Fetches the fertilizer catalogue, ordered by name by the API.
 *
 * Each fertilizer comes with its current stock and whether it is at or below
 * its alert threshold, both computed by the API; neither is computed here.
 */
export function fetchFertilizers() {
    return apiClient.get(PLANTS_ENDPOINTS.FERTILIZERS);
}

/**
 * Adds a fertilizer to the catalogue.
 *
 * The API answers 400 with fieldErrors for an invalid value, and 409 when a
 * fertilizer of the same name exists, ignoring case and accents.
 */
export function createFertilizer(body) {
    return apiClient.post(PLANTS_ENDPOINTS.FERTILIZERS, body);
}

/**
 * Fetches the fertilizer movements, optionally filtered.
 *
 * Same filter rules as fetchVarieties: raw values, empty filters omitted.
 * `movementType` is the API code ("PURCHASE"). The block filter matches
 * applications only, as purchases and losses have no block. `from` and `to`
 * are "YYYY-MM-DD" dates, both included by the API.
 */
export function fetchFertilizerMovements({ fertilizerId, movementType, blockCode, from, to } = {}) {
    const params = {};
    if (fertilizerId !== null && fertilizerId !== undefined) params.fertilizerId = fertilizerId;
    if (movementType) params.movementType = movementType;
    if (blockCode) params.blockCode = blockCode;
    if (from) params.from = from;
    if (to) params.to = to;

    return apiClient.get(PLANTS_ENDPOINTS.FERTILIZER_MOVEMENTS, { params });
}

/**
 * Records a purchase of a fertilizer, in its unit. The cost is optional; the
 * API converts it between FCFA and euros with its stored rate, and gives it
 * only in its own currency while no rate is recorded.
 */
export function createFertilizerPurchase(fertilizerId, body) {
    return apiClient.post(`${PLANTS_ENDPOINTS.FERTILIZERS}/${fertilizerId}/purchases`, body);
}

/**
 * Records an application of a fertilizer on a block. The API answers 422 when
 * the quantity exceeds the current stock.
 */
export function createFertilizerApplication(fertilizerId, body) {
    return apiClient.post(`${PLANTS_ENDPOINTS.FERTILIZERS}/${fertilizerId}/applications`, body);
}

/**
 * Records a loss of a fertilizer. The API answers 422 when the quantity
 * exceeds the current stock.
 */
export function createFertilizerLoss(fertilizerId, body) {
    return apiClient.post(`${PLANTS_ENDPOINTS.FERTILIZERS}/${fertilizerId}/losses`, body);
}

/**
 * Fetches how many FCFA one euro is worth, with its source.
 *
 * The API answers 404 until a rate is entered. That is a normal state, not a
 * failure, so it resolves to null; any other error is passed on.
 */
export function fetchEurXofRate() {
    return apiClient.get(PLANTS_ENDPOINTS.EUR_XOF_RATE).catch((error) => {
        if (error.status === 404) return null;
        throw error;
    });
}

/**
 * Enters the EUR to XOF rate, or replaces it. The API answers 201 for the
 * first rate, 200 for a replacement, and 400 with fieldErrors for an invalid
 * rate.
 */
export function saveEurXofRate(body) {
    return apiClient.put(PLANTS_ENDPOINTS.EUR_XOF_RATE, body);
}

/**
 * Fetches the reference of every variety, ordered by name by the API: yield
 * per tree and harvest months, each with its source.
 */
export function fetchVarietyReferences() {
    return apiClient.get(PLANTS_ENDPOINTS.VARIETY_REFERENCES);
}

/**
 * Enters the reference of a variety. The API answers 400 with fieldErrors for
 * an invalid value, and 409 when a reference of the same name exists, ignoring
 * case and accents.
 */
export function createVarietyReference(body) {
    return apiClient.post(PLANTS_ENDPOINTS.VARIETY_REFERENCES, body);
}

/**
 * Corrects the reference of a variety. A source left out stays as it was when
 * its value does not change, and becomes user_entry when it does. The API
 * answers 400, 404 and 409 as for an entry.
 */
export function updateVarietyReference(id, body) {
    return apiClient.put(`${PLANTS_ENDPOINTS.VARIETY_REFERENCES}/${id}`, body);
}

/**
 * Fetches the yield share of every growth phase, the youngest first: the share
 * in effect, its source, and the default it corrects, if any.
 */
export function fetchGrowthPhaseShares() {
    return apiClient.get(PLANTS_ENDPOINTS.GROWTH_PHASE_YIELD_SHARES);
}

/**
 * Corrects the yield share of a growth phase, named by its code
 * ("GRADUAL_PRODUCTION"). The API answers 200, and 400 with fieldErrors for an
 * invalid share.
 */
export function saveGrowthPhaseShare(code, body) {
    return apiClient.put(`${PLANTS_ENDPOINTS.GROWTH_PHASE_YIELD_SHARES}/${code}`, body);
}
