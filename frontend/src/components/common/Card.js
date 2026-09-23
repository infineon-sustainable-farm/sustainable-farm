import React from 'react';
import './Card.css';

const Card = ({ children, className = '', title, actions, ...props }) => {
  return (
    <div className={`card ${className}`} {...props}>
      {(title || actions) && (
        <div className="card-header">
          {title && <h3 className="card-title">{title}</h3>}
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className="card-body">{children}</div>
    </div>
  );
};

export default Card;