import React, { useState } from 'react';
import Form from './Form';
import FormGroup from './FormGroup';
import Input from './Input';
import Select from './Select';
import './BatchCreationForm.css';

const BatchCreationForm = ({ onSubmit, onCancel, equipmentList, operatorList }) => {
  const [formData, setFormData] = useState({
    batchCode: '',
    harvestEventId: '',
    targetQuantity: '',
    equipmentId: '',
    operatorId: '',
    priority: 'normal',
    notes: ''
  });

  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  const validateField = (name, value) => {
    let error = '';
    
    switch (name) {
      case 'batchCode':
        if (!value.trim()) error = 'Batch code is required';
        else if (!/^[A-Z0-9-]+$/.test(value)) error = 'Batch code must contain only uppercase letters, numbers, and hyphens';
        else if (value.length < 3) error = 'Batch code must be at least 3 characters';
        break;
      case 'harvestEventId':
        if (!value) error = 'Harvest event is required';
        break;
      case 'targetQuantity':
        if (!value) error = 'Target quantity is required';
        else if (isNaN(value) || parseFloat(value) <= 0) error = 'Target quantity must be a positive number';
        break;
      case 'equipmentId':
        if (!value) error = 'Equipment is required';
        break;
      case 'operatorId':
        if (!value) error = 'Operator is required';
        break;
      default:
        break;
    }
    
    return error;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    if (touched[name]) {
      setErrors(prev => ({ ...prev, [name]: validateField(name, value) }));
    }
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    setTouched(prev => ({ ...prev, [name]: true }));
    setErrors(prev => ({ ...prev, [name]: validateField(name, value) }));
  };

  const validateForm = () => {
    const newErrors = {};
    let isValid = true;
    
    Object.keys(formData).forEach(key => {
      const error = validateField(key, formData[key]);
      if (error) {
        newErrors[key] = error;
        isValid = false;
      }
    });
    
    setErrors(newErrors);
    setTouched(Object.keys(formData).reduce((acc, key) => ({ ...acc, [key]: true }), {}));
    return isValid;
  };

  const handleSubmit = () => {
    if (validateForm()) {
      onSubmit(formData);
    }
  };

  const handleReset = () => {
    setFormData({
      batchCode: '',
      harvestEventId: '',
      targetQuantity: '',
      equipmentId: '',
      operatorId: '',
      priority: 'normal',
      notes: ''
    });
    setErrors({});
    setTouched({});
  };

  return (
    <div className="batch-creation-form">
      <div className="form-header">
        <h2>Create New Batch</h2>
        <p className="form-subtitle">Enter batch details to start the production process</p>
      </div>

      <Form onSubmit={handleSubmit}>
        <FormGroup 
          label="Batch Code" 
          required
          error={touched.batchCode && errors.batchCode}
        >
          <Input
            type="text"
            name="batchCode"
            value={formData.batchCode}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="e.g., BATCH-2026-001"
            error={touched.batchCode && errors.batchCode}
          />
          {touched.batchCode && errors.batchCode && (
            <span className="error-message">{errors.batchCode}</span>
          )}
        </FormGroup>

        <FormGroup 
          label="Harvest Event" 
          required
          error={touched.harvestEventId && errors.harvestEventId}
        >
          <Select
            name="harvestEventId"
            value={formData.harvestEventId}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.harvestEventId && errors.harvestEventId}
          >
            <option value="">Select harvest event...</option>
            <option value="1">Harvest Event #1 - 2026-09-01</option>
            <option value="2">Harvest Event #2 - 2026-09-03</option>
            <option value="3">Harvest Event #3 - 2026-09-05</option>
          </Select>
          {touched.harvestEventId && errors.harvestEventId && (
            <span className="error-message">{errors.harvestEventId}</span>
          )}
        </FormGroup>

        <FormGroup 
          label="Target Quantity (kg)" 
          required
          error={touched.targetQuantity && errors.targetQuantity}
        >
          <Input
            type="number"
            name="targetQuantity"
            value={formData.targetQuantity}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="e.g., 1000"
            error={touched.targetQuantity && errors.targetQuantity}
          />
          {touched.targetQuantity && errors.targetQuantity && (
            <span className="error-message">{errors.targetQuantity}</span>
          )}
        </FormGroup>

        <FormGroup 
          label="Equipment" 
          required
          error={touched.equipmentId && errors.equipmentId}
        >
          <Select
            name="equipmentId"
            value={formData.equipmentId}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.equipmentId && errors.equipmentId}
          >
            <option value="">Select equipment...</option>
            {equipmentList?.map(equipment => (
              <option key={equipment.id} value={equipment.id}>
                {equipment.name} - {equipment.status}
              </option>
            ))}
          </Select>
          {touched.equipmentId && errors.equipmentId && (
            <span className="error-message">{errors.equipmentId}</span>
          )}
        </FormGroup>

        <FormGroup 
          label="Operator" 
          required
          error={touched.operatorId && errors.operatorId}
        >
          <Select
            name="operatorId"
            value={formData.operatorId}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.operatorId && errors.operatorId}
          >
            <option value="">Select operator...</option>
            {operatorList?.map(operator => (
              <option key={operator.id} value={operator.id}>
                {operator.name} - {operator.role}
              </option>
            ))}
          </Select>
          {touched.operatorId && errors.operatorId && (
            <span className="error-message">{errors.operatorId}</span>
          )}
        </FormGroup>

        <FormGroup label="Priority">
          <Select
            name="priority"
            value={formData.priority}
            onChange={handleChange}
          >
            <option value="low">Low</option>
            <option value="normal">Normal</option>
            <option value="high">High</option>
            <option value="urgent">Urgent</option>
          </Select>
        </FormGroup>

        <FormGroup label="Notes">
          <Input
            type="text"
            name="notes"
            value={formData.notes}
            onChange={handleChange}
            placeholder="Additional notes or instructions..."
          />
        </FormGroup>

        <div className="form-actions">
          <button 
            type="button" 
            onClick={handleReset}
            className="btn-secondary"
          >
            Reset Form
          </button>
          <button 
            type="button" 
            onClick={onCancel}
            className="btn-secondary"
          >
            Cancel
          </button>
          <button 
            type="submit" 
            className="btn-primary"
          >
            Create Batch
          </button>
        </div>
      </Form>
    </div>
  );
};

export default BatchCreationForm;