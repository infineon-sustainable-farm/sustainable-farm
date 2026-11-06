import React from 'react';
import './Badge.css';

const Badge = ({ variant = 'info', children }) => {
  return <span className={`badge badge-${variant}`}>{children}</span>;
};

export default Badge;