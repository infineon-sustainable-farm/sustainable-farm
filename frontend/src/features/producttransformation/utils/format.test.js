import { describe, expect, it } from 'vitest';
import i18n from '../../../shared/i18n/i18n';
import {
  formatDate,
  formatDateTime,
  formatEnum,
  formatIsoDate,
  formatNumber,
  formatWeight,
  formatPercentage,
  getStatusTone,
  getQcResultTone,
  getComplianceTone,
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

  it('reads date-only values as local dates', () => {
    expect(formatIsoDate('2026-10-01')).toBe('Oct 1, 2026');
    expect(formatIsoDate('')).toBe('N/A');
    expect(formatIsoDate('soon')).toBe('soon');
  });

  it('formats numbers, weights and percentages with defaults and fallbacks', () => {
    expect(formatNumber(null)).toBe('N/A');
    expect(formatNumber(undefined)).toBe('N/A');
    expect(formatNumber(12.345)).toBe('12.35');
    expect(formatNumber(12.345, 1)).toBe('12.3');
    expect(formatNumber(1250)).toBe('1250.00');

    expect(formatWeight(null)).toBe('N/A');
    expect(formatWeight(480.5)).toBe('480.50 kg');

    expect(formatPercentage(null)).toBe('N/A');
    expect(formatPercentage(92.346)).toBe('92.35%');
  });

  it('follows the active language', async () => {
    await i18n.changeLanguage('fr');
    expect(formatWeight(480.5)).toBe('480,50 kg');
    expect(formatPercentage(92.346)).toBe('92,35 %');
    expect(formatNumber(null)).toBe('N/D');
    await i18n.changeLanguage('de');
    expect(formatIsoDate('2026-10-01')).toBe('1. Okt. 2026');
    expect(formatNumber(null)).toBe('k. A.');
  });

  it('translates enum codes and keeps unknown codes readable', () => {
    expect(formatEnum('qcResult', 'PASS')).toBe('Pass');
    expect(formatEnum('qcResult', 'SOMETHING_NEW')).toBe('Something New');
    expect(formatEnum('qcResult', null)).toBe('N/A');
  });

  it('maps every batch status to a tone and falls back for unknown values', () => {
    expect(getStatusTone('CREATED')).toBe('neutral');
    expect(getStatusTone('INTAKE')).toBe('info');
    expect(getStatusTone('WASHING')).toBe('info');
    expect(getStatusTone('DRYING')).toBe('warning');
    expect(getStatusTone('PACKAGING')).toBe('warning');
    expect(getStatusTone('COMPLETED')).toBe('success');
    expect(getStatusTone('SHIPPED')).toBe('success');
    expect(getStatusTone('REJECTED')).toBe('error');
    expect(getStatusTone('UNKNOWN')).toBe('neutral');
  });

  it('maps QC and compliance results and falls back for unknown values', () => {
    expect(getQcResultTone('PASS')).toBe('success');
    expect(getQcResultTone('FAIL')).toBe('error');
    expect(getQcResultTone('REWORK')).toBe('warning');
    expect(getQcResultTone('PENDING')).toBe('neutral');
    expect(getQcResultTone('OTHER')).toBe('neutral');

    expect(getComplianceTone('COMPLIANT')).toBe('success');
    expect(getComplianceTone('NON_COMPLIANT')).toBe('error');
    expect(getComplianceTone('PENDING')).toBe('neutral');
    expect(getComplianceTone('OTHER')).toBe('neutral');
  });

  it('truncates long text and leaves short text untouched', () => {
    expect(truncate('')).toBe('');
    expect(truncate(null)).toBe('');
    expect(truncate('short', 10)).toBe('short');
    expect(truncate('abcdefghij', 5)).toBe('abcde...');
  });
});
