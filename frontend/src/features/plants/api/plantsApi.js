import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

/**
 * Fetches the varieties, optionally filtered.
 *
 * Filters are sent as raw domain values: the block goes out as "A", not as the
 * "Block A" label used on screen. An omitted or empty filter is left out of the
 * query string entirely so the API returns every row.
 */
export function fetchVarieties({ bloc_parcelle, id_ferme } = {}) {
    const params = {};
    if (bloc_parcelle) params.bloc_parcelle = bloc_parcelle;
    if (id_ferme !== null && id_ferme !== undefined) params.id_ferme = id_ferme;

    return apiClient.get(ENDPOINTS.VARIETIES, { params });
}

/**
 * Fetches a single variety by its identifier.
 */
export function fetchVarietyById(id) {
    return apiClient.get(`${ENDPOINTS.VARIETIES}/${id}`);
}
