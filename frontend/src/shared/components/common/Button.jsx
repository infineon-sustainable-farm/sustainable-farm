import React from 'react';
import './Button.css';

const Button = ({ variant = 'primary', size = 'medium', children, ...props }) => {
  return (
    <button className={`btn btn-${variant} btn-${size}`} {...props}>
      {children}
    </button>
  );
};

export default Button;