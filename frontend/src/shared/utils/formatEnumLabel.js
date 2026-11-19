/**
 * Turns a backend enum value into a readable label.
 *
 * Words are title-cased (`POST_WASH` -> "Post Wash"), but known acronyms keep
 * their capitals: "QC_INSPECTOR" reads "QC Inspector", not "Qc Inspector", and
 * "GLOBALGAP" is not split into "Globalgap". The backend values themselves are
 * never changed; only the display is.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.1.0
 */

/** Acronyms the modules actually display. Kept short on purpose. */
const ACRONYMS = new Set([
  'QC',
  'QA',
  'HACCP',
  'GLOBALGAP',
  'ISO',
  'PPE',
  'KPI',
  'N/A',
]);

export function formatEnumLabel(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .split('_')
    .filter(Boolean)
    .map((word) => {
      const upper = word.toUpperCase();
      if (ACRONYMS.has(upper)) return upper;
      return word[0] + word.slice(1).toLowerCase();
    })
    .join(' ');
}
