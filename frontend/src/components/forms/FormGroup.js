import React from 'react';
import './FormGroup.css';

const FormGroup = ({ label, required, children }) => {
  return (
    <div className="form-group">
      {label && (
        <label className="form-label">
          {label}
          {required && <span className="required-indicator">*</span>}
        </label>
      )}
      <div className="form-input-container">{children}</div>
    </div>
  );
};

export default FormGroup;