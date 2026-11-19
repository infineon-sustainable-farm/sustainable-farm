import { apiClient } from './client'

/** Read-only list of the farm people directory (owner: visitor management Staff screen). */
export const PERSONS_ENDPOINT = '/api/staff/persons'

/**
 * Active people of the directory, by full name.
 *
 * @param {string} [search] - keeps the people whose name or email contains it.
 * @returns {Promise<Array<{ id: number, fullName: string, email?: string, phone?: string, active: boolean }>>}
 */
export function fetchPersons(search) {
  const params = search && search.trim() ? { search: search.trim() } : undefined
  return apiClient.get(PERSONS_ENDPOINT, params ? { params } : undefined)
}
