/**
 * Product Transformation - Operators (read-only)
 *
 * The other PT screens already reference operators (drying, QC, compliance,
 * packaging select one), and the backend exposes /api/operators, but the module
 * had no screen to see them. This is a consultation view on purpose: operators
 * are master data owned by the backend seed for the demo, so there is no
 * create/edit/delete here.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { formatIsoDate } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { Filter, Search } from 'lucide-react';

import PTListHeader from './PTListHeader';
import { CARD_FRAME, FIELD_LABEL, FILTER_INPUT, SEARCH_INPUT, SECTION_CARD, TABLE_CARD, TABLE_HEAD } from '../styles/ptStyles';

const EMPTY_FILTERS = {
  role: 'all',
  status: 'all',
  search: '',
};

const STATUS_BADGES = {
  ACTIVE: 'bg-green-100 text-green-800',
  INACTIVE: 'bg-gray-100 text-gray-700',
};

const OperatorList = () => {
  const [operators, setOperators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await operatorApi.getAllOperators();
      setOperators(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  // Roles are read from the data so the filter never drifts from the backend enum.
  const roles = useMemo(
    () => [...new Set(operators.map((operator) => operator.role).filter(Boolean))].sort(),
    [operators],
  );

  const filteredOperators = useMemo(() => {
    const search = filters.search.trim().toLowerCase();
    return operators
      .filter((operator) => {
        if (filters.role !== 'all' && operator.role !== filters.role) return false;
        if (filters.status !== 'all' && operator.activeStatus !== filters.status) return false;
        if (
          search &&
          !`${operator.operatorId} ${operator.operatorName} ${operator.certifications || ''}`
            .toLowerCase()
            .includes(search)
        ) {
          return false;
        }
        return true;
      })
      .sort((a, b) => String(a.operatorId).localeCompare(String(b.operatorId)));
  }, [operators, filters]);

  const updateFilter = (key) => (event) => {
    setFilters((current) => ({ ...current, [key]: event.target.value }));
  };

  const header = (
    <PTListHeader
      title="Operators"
      subtitle="Process operators referenced by drying, quality control, compliance and packaging."
    />
  );

  if (loading) return <LoadingState message="Loading operators..." />;
  if (error)
    return (
      <div className="p-6">
        {header}
        <ErrorState message={error?.message || 'Failed to load operators'} onRetry={load} />
      </div>
    );

  return (
    <div className="p-6">
      {header}

      {/* Filters */}
      <div className={`${SECTION_CARD} mb-6`}>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex items-center gap-2 text-gray-500">
            <Filter className="w-5 h-5" />
            <span className="text-sm font-medium">Filters</span>
          </div>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Role
            <select
              aria-label="Filter by role"
              value={filters.role}
              onChange={updateFilter('role')}
              className={FILTER_INPUT}
            >
              <option value="all">All roles</option>
              {roles.map((role) => (
                <option key={role} value={role}>
                  {formatEnumLabel(role)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Status
            <select
              aria-label="Filter by status"
              value={filters.status}
              onChange={updateFilter('status')}
              className={FILTER_INPUT}
            >
              <option value="all">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Search
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                aria-label="Search operators"
                placeholder="ID, name or certification"
                value={filters.search}
                onChange={updateFilter('search')}
                className={SEARCH_INPUT}
              />
            </div>
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredOperators.length} operator{filteredOperators.length === 1 ? '' : 's'}
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredOperators.length === 0 ? (
        <div className={CARD_FRAME}>
          <EmptyState
            message={
              operators.length === 0
                ? 'No operator registered'
                : 'No operator matches the filters'
            }
          />
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                {['Operator', 'Name', 'Role', 'Certifications', 'Status', 'Hire date'].map(
                  (label) => (
                    <th
                      key={label}
                      className={TABLE_HEAD}
                    >
                      {label}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredOperators.map((operator) => (
                <tr key={operator.operatorId} className="hover:bg-gray-50">
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {operator.operatorId}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {operator.operatorName}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatEnumLabel(operator.role || 'UNKNOWN')}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {operator.certifications || 'N/A'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        STATUS_BADGES[operator.activeStatus] || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {formatEnumLabel(operator.activeStatus || 'UNKNOWN')}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatIsoDate(operator.hireDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default OperatorList;
