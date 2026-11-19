// Grades accepted by /api/product-transformation/raw-intakes. The varieties come from the plants
// catalogue (api/varietyApi.js), picked by their key.
export const QUALITY_GRADES = ['A', 'B', 'C', 'D'];

/**
 * Catalogue key of a variety as the API returns it: the API sends `AMÉLIE` for the key `AMELIE`,
 * the key itself for every other variety.
 */
export const varietyKeyOf = (variety) =>
  variety
    ? String(variety).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase()
    : '';
