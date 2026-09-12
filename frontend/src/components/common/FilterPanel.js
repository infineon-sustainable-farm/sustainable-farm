import React, { useState } from 'react';
import Select from '../forms/Select';
import Input from '../forms/Input';
import './FilterPanel.css';

const FilterPanel = ({ 
  onFilterChange, 
  filters = [],
  initialFilters = {},
  onClearFilters 
}) => {
  const [activeFilters, setActiveFilters] = useState(initialFilters);
  const [isExpanded, setIsExpanded] = useState(false);

  const handleFilterChange = (filterName, value) => {
    const newFilters = { ...activeFilters, [filterName]: value };
    setActiveFilters(newFilters);
    onFilterChange(newFilters);
  };

  const handleClearAll = () => {
    const clearedFilters = {};
    filters.forEach(filter => {
      clearedFilters[filter.name] = filter.defaultValue || '';
    });
    setActiveFilters(clearedFilters);
    onFilterChange(clearedFilters);
    if (onClearFilters) {
      onClearFilters();
    }
  };

  const hasActiveFilters = Object.values(activeFilters).some(
    value => value !== '' && value !== null && value !== undefined
  );

  const activeFilterCount = Object.values(activeFilters).filter(
    value => value !== '' && value !== null && value !== undefined
  ).length;

  return (
    <div className="filter-panel">
      <div className="filter-header">
        <button 
          className="filter-toggle-button"
          onClick={() => setIsExpanded(!isExpanded)}
        >
          <svg 
            width="20" 
            height="20" 
            viewBox="0 0 24 24" 
            fill="none" 
            stroke="currentColor" 
            strokeWidth="2"
            className={`filter-icon ${isExpanded ? 'expanded' : ''}`}
          >
            <path d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          <span>Filters</span>
          {activeFilterCount > 0 && (
            <span className="filter-count">{activeFilterCount}</span>
          )}
        </button>
        
        {hasActiveFilters && (
          <button 
            className="clear-filters-button"
            onClick={handleClearAll}
          >
            Clear All
          </button>
        )}
      </div>

      {isExpanded && (
        <div className="filter-content">
          <div className="filter-grid">
            {filters.map(filter => (
              <div key={filter.name} className="filter-item">
                <label className="filter-label">
                  {filter.label}
                  {filter.required && <span className="required-indicator">*</span>}
                </label>
                
                {filter.type === 'select' && (
                  <Select
                    value={activeFilters[filter.name] || filter.defaultValue || ''}
                    onChange={(e) => handleFilterChange(filter.name, e.target.value)}
                  >
                    {filter.placeholder && (
                      <option value="">{filter.placeholder}</option>
                    )}
                    {filter.options?.map(option => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </Select>
                )}
                
                {filter.type === 'text' && (
                  <Input
                    type="text"
                    value={activeFilters[filter.name] || filter.defaultValue || ''}
                    onChange={(e) => handleFilterChange(filter.name, e.target.value)}
                    placeholder={filter.placeholder}
                  />
                )}
                
                {filter.type === 'date' && (
                  <Input
                    type="date"
                    value={activeFilters[filter.name] || filter.defaultValue || ''}
                    onChange={(e) => handleFilterChange(filter.name, e.target.value)}
                  />
                )}
                
                {filter.type === 'number' && (
                  <Input
                    type="number"
                    value={activeFilters[filter.name] || filter.defaultValue || ''}
                    onChange={(e) => handleFilterChange(filter.name, e.target.value)}
                    placeholder={filter.placeholder}
                  />
                )}
                
                {filter.type === 'date-range' && (
                  <div className="date-range-inputs">
                    <Input
                      type="date"
                      value={activeFilters[`${filter.name}_from`] || ''}
                      onChange={(e) => handleFilterChange(`${filter.name}_from`, e.target.value)}
                      placeholder="From"
                    />
                    <Input
                      type="date"
                      value={activeFilters[`${filter.name}_to`] || ''}
                      onChange={(e) => handleFilterChange(`${filter.name}_to`, e.target.value)}
                      placeholder="To"
                    />
                  </div>
                )}
              </div>
            ))}
          </div>
          
          <div className="filter-actions">
            <button 
              className="apply-filters-button"
              onClick={() => onFilterChange(activeFilters)}
            >
              Apply Filters
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FilterPanel;