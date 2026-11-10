import { describe, expect, it } from 'vitest';
import {
  formatDate,
  formatDateTime,
  formatNumber,
  formatWeight,
  formatPercentage,
  getStatusColor,
  getQcResultColor,
  getComplianceColor,
  truncate,
} from './format';

describe('format utilities', () => {
  it('formats dates and datetimes, with an N/A fallback', () => {
    expect(formatDate(null)).toBe('N/A');
    expect(formatDate('2026-10-01T12:00:00')).toMatch(/Oct 1, 2026/);

    expect(formatDateTime(undefined)).toBe('N/A');
    const dateTime = formatDateTime('2026-10-01T12:00:00');
    expect(dateTime).toMatch(/Oct 1, 2026/);
    expect(dateTime).toMatch(/12:00/);
  });

  it('formats numbers, weights and percentages with defaults and fallbacks', () => {
    expect(formatNumber(null)).toBe('N/A');
    expect(formatNumber(undefined)).toBe('N/A');
    expect(formatNumber(12.345)).toBe('12.35');
    expect(formatNumber(12.345, 1)).toBe('12.3');

    expect(formatWeight(null)).toBe('N/A');
    expect(formatWeight(480.5)).toBe('480.50 kg');

    expect(formatPercentage(null)).toBe('N/A');
    expect(formatPercentage(92.346)).toBe('92.35%');
  });

  it('maps every batch status to a colour and falls back for unknown values', () => {
    expect(getStatusColor('CREATED')).toBe('bg-gray-100 text-gray-800');
    expect(getStatusColor('INTAKE')).toBe('bg-blue-100 text-blue-800');
    expect(getStatusColor('WASHING')).toBe('bg-cyan-100 text-cyan-800');
    expect(getStatusColor('DRYING')).toBe('bg-yellow-100 text-yellow-800');
    expect(getStatusColor('PACKAGING')).toBe('bg-orange-100 text-orange-800');
    expect(getStatusColor('COMPLETED')).toBe('bg-green-100 text-green-800');
    expect(getStatusColor('SHIPPED')).toBe('bg-purple-100 text-purple-800');
    expect(getStatusColor('REJECTED')).toBe('bg-red-100 text-red-800');
    expect(getStatusColor('UNKNOWN')).toBe('bg-gray-100 text-gray-800');
  });

  it('maps QC and compliance results and falls back for unknown values', () => {
    expect(getQcResultColor('PASS')).toBe('bg-green-100 text-green-800');
    expect(getQcResultColor('FAIL')).toBe('bg-red-100 text-red-800');
    expect(getQcResultColor('REWORK')).toBe('bg-yellow-100 text-yellow-800');
    expect(getQcResultColor('PENDING')).toBe('bg-gray-100 text-gray-800');
    expect(getQcResultColor('OTHER')).toBe('bg-gray-100 text-gray-800');

    expect(getComplianceColor('COMPLIANT')).toBe('bg-green-100 text-green-800');
    expect(getComplianceColor('NON_COMPLIANT')).toBe('bg-red-100 text-red-800');
    expect(getComplianceColor('PENDING')).toBe('bg-gray-100 text-gray-800');
    expect(getComplianceColor('OTHER')).toBe('bg-gray-100 text-gray-800');
  });

  it('truncates long text and leaves short text untouched', () => {
    expect(truncate('')).toBe('');
    expect(truncate(null)).toBe('');
    expect(truncate('short', 10)).toBe('short');
    expect(truncate('abcdefghij', 5)).toBe('abcde...');
  });
});
