import { formatEnum } from '../utils/format';

// Values accepted by /api/raw-intakes for the received variety and grade.
export const MANGO_VARIETIES = ['KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER'];
export const QUALITY_GRADES = ['A', 'B', 'C', 'D'];

/** Translated label of a received variety. */
export const varietyLabel = (variety) => formatEnum('variety', variety);
