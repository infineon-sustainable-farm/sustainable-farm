import React from 'react';
import './Input.css';

const Input = ({ 
  type = 'text', 
  placeholder = '', 
  value = '', 
  onChange, 
  disabled = false, 
  error = false,
  ...props 
}) => {
  return (
    <input
      type={type}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      disabled={disabled}
      className={`form-input ${error ? 'input-error' : ''}`}
      {...props}
    />
  );
};

export default Input;