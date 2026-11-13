import { describe, expect, it } from 'vitest';
import { toCsv } from './chartExport';

describe('toCsv', () => {
  const columns = [
    { key: 'batchId', label: 'Batch' },
    { key: 'quantityKg', label: 'Quantity (kg)' },
    { key: 'share', label: 'Share' },
  ];

  it('keeps numeric values machine-readable instead of localizing them', () => {
    const csv = toCsv([{ batchId: 'B-001', quantityKg: 12500.5, share: 0.42 }], columns);

    // Display strings such as "12,500.5" or "42%" would break spreadsheet parsers.
    expect(csv).toContain('B-001;12500.5;0.42');
    expect(csv).not.toContain('12,500.5');
    expect(csv).not.toContain('%');
  });

  it('writes a BOM header and leaves missing values empty', () => {
    const csv = toCsv([{ batchId: 'B-001', quantityKg: null, share: undefined }], columns);

    expect(csv.startsWith('\uFEFFBatch;Quantity (kg);Share\r\n')).toBe(true);
    expect(csv).toContain('B-001;;');
  });

  it('quotes values containing the separator or quotes', () => {
    const csv = toCsv([{ batchId: 'B;001', quantityKg: 'a"b', share: 1 }], columns);

    expect(csv).toContain('"B;001";"a""b";1');
  });
});
