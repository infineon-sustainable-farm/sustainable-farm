/**
 * Shared utility functions for data formatting
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

/**
 * Format a date-only value (YYYY-MM-DD) without shifting across time zones.
 * `new Date("2026-01-15")` is parsed as UTC and can display the previous day.
 */
export const formatIsoDate = (value) => {
  if (!value) return 'N/A';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  if (!year || !month || !day) return String(value);
  return new Date(Number(year), Number(month) - 1, Number(day)).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Format date to readable string
 */
export const formatDate = (date) => {
  if (!date) return 'N/A';
  const d = new Date(date);
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Format date and time to readable string
 */
export const formatDateTime = (date) => {
  if (!date) return 'N/A';
  const d = new Date(date);
  return d.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Format a number the way the Plants module does: `en-US` grouping and no
 * forced trailing zeros.
 *
 * `decimals` is a ceiling, not a fixed width, so a whole quantity stays
 * "12,500" and 480.5 stays "480.5" while 8,420.25 keeps both of its meaningful
 * decimals. Display only: CSV exports keep the raw numeric values.
 */
export const formatNumber = (num, decimals = 2) => {
  if (num === null || num === undefined || num === '') return 'N/A';
  const value = Number(num);
  if (!Number.isFinite(value)) return 'N/A';
  return value.toLocaleString('en-US', {
    maximumFractionDigits: decimals,
    minimumFractionDigits: 0,
  });
};

/**
 * Format weight in kg
 */
export const formatWeight = (kg) => {
  if (kg === null || kg === undefined) return 'N/A';
  return `${formatNumber(kg)} kg`;
};

/**
 * Format percentage
 */
export const formatPercentage = (value) => {
  if (value === null || value === undefined) return 'N/A';
  return `${formatNumber(value)}%`;
};

/**
 * Format status with color mapping
 */
export const getStatusColor = (status) => {
  const statusColors = {
    CREATED: 'bg-gray-100 text-gray-800',
    INTAKE: 'bg-blue-100 text-blue-800',
    WASHING: 'bg-cyan-100 text-cyan-800',
    DRYING: 'bg-yellow-100 text-yellow-800',
    PACKAGING: 'bg-orange-100 text-orange-800',
    COMPLETED: 'bg-green-100 text-green-800',
    SHIPPED: 'bg-purple-100 text-purple-800',
    REJECTED: 'bg-red-100 text-red-800',
  };
  return statusColors[status] || 'bg-gray-100 text-gray-800';
};

/**
 * Format QC result with color mapping
 */
export const getQcResultColor = (result) => {
  const resultColors = {
    PASS: 'bg-green-100 text-green-800',
    FAIL: 'bg-red-100 text-red-800',
    REWORK: 'bg-yellow-100 text-yellow-800',
    PENDING: 'bg-gray-100 text-gray-800',
  };
  return resultColors[result] || 'bg-gray-100 text-gray-800';
};

/**
 * Format compliance result with color mapping
 */
export const getComplianceColor = (result) => {
  const complianceColors = {
    COMPLIANT: 'bg-green-100 text-green-800',
    NON_COMPLIANT: 'bg-red-100 text-red-800',
    PENDING: 'bg-gray-100 text-gray-800',
  };
  return complianceColors[result] || 'bg-gray-100 text-gray-800';
};

/**
 * Truncate text with ellipsis
 */
export const truncate = (text, maxLength = 50) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return `${text.substring(0, maxLength)}...`;
};
