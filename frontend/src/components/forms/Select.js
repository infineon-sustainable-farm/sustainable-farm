import React from 'react';
import './Select.css';

const Select = ({ value = '', onChange, disabled = false, error = false, options = [], placeholder, children, ...props }) => {
  return (
    <select
      value={value}
      onChange={onChange}
      disabled={disabled}
      className={`form-select ${error ? 'input-error' : ''}`}
      {...props}
    >
      {placeholder && <option value="">{placeholder}</option>}
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
      {children}
    </select>
  );
};

export default Select;