import React from 'react';
import './LoadingSkeleton.css';

const LoadingSkeleton = ({ 
  variant = 'text', 
  width = '100%', 
  height = '20px', 
  count = 1,
  className = '' 
}) => {
  const skeletons = Array.from({ length: count }, (_, index) => (
    <div
      key={index}
      className={`skeleton skeleton-${variant} ${className}`}
      style={{ width, height }}
    />
  ));

  return <div className="skeleton-container">{skeletons}</div>;
};

// Pre-configured skeleton components for common use cases
const TableSkeleton = ({ rows = 5, columns = 6 }) => {
  return (
    <div className="table-skeleton">
      {/* Header */}
      <div className="table-skeleton-header">
        {Array.from({ length: columns }, (_, index) => (
          <LoadingSkeleton key={`header-${index}`} variant="rectangular" height="40px" />
        ))}
      </div>
      {/* Rows */}
      {Array.from({ length: rows }, (_, rowIndex) => (
        <div key={`row-${rowIndex}`} className="table-skeleton-row">
          {Array.from({ length: columns }, (_, colIndex) => (
            <LoadingSkeleton key={`cell-${rowIndex}-${colIndex}`} variant="text" height="20px" />
          ))}
        </div>
      ))}
    </div>
  );
};

const CardSkeleton = () => {
  return (
    <div className="card-skeleton">
      <LoadingSkeleton variant="rectangular" height="120px" width="100%" />
    </div>
  );
};

const KPICardSkeleton = ({ count = 4 }) => {
  return (
    <div className="kpi-skeleton-grid">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="kpi-skeleton">
          <LoadingSkeleton variant="text" height="16px" width="60%" />
          <LoadingSkeleton variant="text" height="32px" width="80%" />
          <LoadingSkeleton variant="text" height="14px" width="40%" />
        </div>
      ))}
    </div>
  );
};

const FormSkeleton = ({ fieldCount = 4 }) => {
  return (
    <div className="form-skeleton">
      {Array.from({ length: fieldCount }, (_, index) => (
        <div key={index} className="form-skeleton-field">
          <LoadingSkeleton variant="text" height="16px" width="30%" />
          <LoadingSkeleton variant="rectangular" height="40px" width="100%" />
        </div>
      ))}
      <div className="form-skeleton-actions">
        <LoadingSkeleton variant="rectangular" height="40px" width="120px" />
        <LoadingSkeleton variant="rectangular" height="40px" width="120px" />
      </div>
    </div>
  );
};

export default LoadingSkeleton;
export { TableSkeleton, CardSkeleton, KPICardSkeleton, FormSkeleton };