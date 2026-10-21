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

/**
 * Fetches the catalogue of pests and diseases, in the order of the API: the
 * pests, then the diseases, then "Other", each by name. Outside the dev
 * profile it starts empty, and "Other" is listed only once an inspection or a
 * treatment has used it.
 */
export function fetchHealthIssues() {
    return apiClient.get(PLANTS_ENDPOINTS.HEALTH_ISSUES);
}

/**
 * Adds a pest or a disease to the catalogue. The API derives its code from the
 * name ("Powdery mildew" gives POWDERY_MILDEW), answers 400 with fieldErrors
 * for an invalid value, and 409 when the name, ignoring case and accents, or
 * the code is already in the catalogue.
 */
export function createHealthIssue(body) {
    return apiClient.post(PLANTS_ENDPOINTS.HEALTH_ISSUES, body);
}

/**
 * Fetches the current health of each inspected block: the score, date and
 * category of its latest inspection, all from the API. This route takes no
 * block filter, and a block never inspected is not listed.
 */
export function fetchBlockHealth({ farmId } = {}) {
    const params = {};
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;

    return apiClient.get(PLANTS_ENDPOINTS.BLOCK_HEALTH, { params });
}

/**
 * Records an inspection of a block with the problems found, possibly none. The
 * API writes the inspection and its findings together or not at all: it
 * answers 400 with fieldErrors, those of a finding under its index
 * ("findings[0].otherLabel"), and 422 when an issue code is not in the
 * catalogue.
 */
export function createHealthInspection(body) {
    return apiClient.post(PLANTS_ENDPOINTS.HEALTH_INSPECTIONS, body);
}

/**
 * Fetches the findings, optionally filtered, by inspection date. Same filter
 * rules as fetchVarieties; `status` is one API code ("UNTREATED"), and the
 * block and both dates are those of the inspection. The status and the date
 * from which harvesting is allowed come computed by the API.
 */
export function fetchHealthFindings({ blockCode, farmId, status, from, to } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;
    if (status) params.status = status;
    if (from) params.from = from;
    if (to) params.to = to;

    return apiClient.get(PLANTS_ENDPOINTS.HEALTH_FINDINGS, { params });
}

/**
 * Records a treatment answering a finding. The farm, the block and the problem
 * targeted are copied from the finding by the API, which answers 400 with
 * fieldErrors, 404 for an unknown finding, and 422 when the treatment predates
 * the inspection or the finding is resolved.
 */
export function createFindingTreatment(findingId, body) {
    return apiClient.post(`${PLANTS_ENDPOINTS.HEALTH_FINDINGS}/${findingId}/treatments`, body);
}

/**
 * Resolves a finding, once. The API answers 400 with fieldErrors, 404 for an
 * unknown finding, 409 when it is already resolved, and 422 when the date
 * predates the inspection or the last treatment.
 */
export function resolveHealthFinding(findingId, body) {
    return apiClient.post(`${PLANTS_ENDPOINTS.HEALTH_FINDINGS}/${findingId}/resolution`, body);
}

/**
 * Records a preventive treatment, answering no finding. The API answers 400
 * with fieldErrors and 422 when the problem targeted is not in the catalogue.
 */
export function createPreventiveTreatment(body) {
    return apiClient.post(PLANTS_ENDPOINTS.TREATMENTS, body);
}

/**
 * Fetches the treatment record, optionally filtered, by treatment date: the
 * treatments answering a finding and the preventive ones together. Same filter
 * rules as fetchVarieties. The date from which harvesting is allowed comes
 * computed by the API.
 */
export function fetchTreatments({ blockCode, farmId, from, to } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;
    if (from) params.from = from;
    if (to) params.to = to;

    return apiClient.get(PLANTS_ENDPOINTS.TREATMENTS, { params });
}

/**
 * Fetches the plant alerts of the day, computed by the API: CRITICAL first,
 * then by date. `withinDays` only sets how early a harvest season is
 * announced; it is left out to keep the API's default.
 */
export function fetchPlantAlerts({ blockCode, farmId, withinDays } = {}) {
    const params = {};
    if (blockCode) params.blockCode = blockCode;
    if (farmId !== null && farmId !== undefined) params.farmId = farmId;
    if (withinDays !== null && withinDays !== undefined) params.withinDays = withinDays;

    return apiClient.get(PLANTS_ENDPOINTS.ALERTS, { params });
}

/**
 * Fetches the nursery batches, ordered by start date by the API. The plants
 * left, the survival rate and the current stage of each batch come computed
 * by the API from its recorded events; none of them is computed here.
 */
export function fetchNurseryBatches() {
    return apiClient.get(PLANTS_ENDPOINTS.NURSERY_BATCHES);
}

/**
 * Starts a nursery batch. The API records its initial stage as its first
 * stage change, and answers 400 with fieldErrors for an invalid value, 409
 * when the code is already taken, ignoring case, and 422 when the planned
 * transplant date is before the start date.
 */
export function createNurseryBatch(body) {
    return apiClient.post(PLANTS_ENDPOINTS.NURSERY_BATCHES, body);
}

/**
 * Fetches the nursery events, optionally filtered, by date: stage changes,
 * losses and transplants. Same filter rules as fetchVarieties; `eventType` is
 * the API code ("LOSS"), and `from` and `to` are "YYYY-MM-DD" dates, both
 * included by the API.
 */
export function fetchNurseryEvents({ batchId, eventType, from, to } = {}) {
    const params = {};
    if (batchId !== null && batchId !== undefined) params.batchId = batchId;
    if (eventType) params.eventType = eventType;
    if (from) params.from = from;
    if (to) params.to = to;

    return apiClient.get(PLANTS_ENDPOINTS.NURSERY_EVENTS, { params });
}
