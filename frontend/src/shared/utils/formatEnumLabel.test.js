import { describe, expect, it } from 'vitest';
import { formatEnumLabel } from './formatEnumLabel';

describe('formatEnumLabel', () => {
  it('title-cases backend enum values', () => {
    expect(formatEnumLabel('POST_WASH')).toBe('Post Wash');
    expect(formatEnumLabel('FOOD_SAFETY')).toBe('Food Safety');
    expect(formatEnumLabel('IN_STOCK')).toBe('In Stock');
  });

  it('keeps known acronyms in capitals', () => {
    expect(formatEnumLabel('QC_INSPECTOR')).toBe('QC Inspector');
    expect(formatEnumLabel('QA')).toBe('QA');
    expect(formatEnumLabel('HACCP')).toBe('HACCP');
    expect(formatEnumLabel('GLOBALGAP')).toBe('GLOBALGAP');
    expect(formatEnumLabel('ISO_22000')).toBe('ISO 22000');
    expect(formatEnumLabel('PPE_REQUIRED')).toBe('PPE Required');
  });

  it('returns an empty string for a missing value', () => {
    expect(formatEnumLabel(null)).toBe('');
    expect(formatEnumLabel(undefined)).toBe('');
  });
});
