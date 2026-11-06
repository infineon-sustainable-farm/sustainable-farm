import React from 'react';
import './Form.css';

const Form = ({ onSubmit, children }) => {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <form onSubmit={handleSubmit} className="form-grid">
      {children}
    </form>
  );
};

export default Form;