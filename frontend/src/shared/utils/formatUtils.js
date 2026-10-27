/**
 * Shared utility functions for data formatting
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

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
 * Format number with decimal places
 */
export const formatNumber = (num, decimals = 2) => {
  if (num === null || num === undefined) return 'N/A';
  return Number(num).toFixed(decimals);
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
