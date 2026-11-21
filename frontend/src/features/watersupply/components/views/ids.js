/**
 * Compares two ids whatever their type: the API returns numeric ids for farms, fields, zones and
 * sources, while form controls (select, input) hold them as strings.
 */
export const sameId = (a, b) => a != null && b != null && a !== '' && b !== '' && String(a) === String(b)
