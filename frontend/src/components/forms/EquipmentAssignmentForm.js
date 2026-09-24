import React, { useState } from 'react';
import Form from './Form';
import FormGroup from './FormGroup';
import Input from './Input';
import Select from './Select';
import './EquipmentAssignmentForm.css';

const EquipmentAssignmentForm = ({ onSubmit, onCancel, batchList, equipmentList }) => {
  const [formData, setFormData] = useState({
    batchId: '',
    equipmentId: '',
    assignmentType: 'primary',
    startTime: '',
    endTime: '',
    notes: ''
  });

  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  const validateField = (name, value) => {
    let error = '';
    
    switch (name) {
      case 'batchId':
        if (!value) error = 'Batch is required';
        break;
      case 'equipmentId':
        if (!value) error = 'Equipment is required';
        break;
      case 'startTime':
        if (!value) error = 'Start time is required';
        break;
      case 'endTime':
        if (value && formData.startTime && new Date(value) <= new Date(formData.startTime)) {
          error = 'End time must be after start time';
        }
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
      batchId: '',
      equipmentId: '',
      assignmentType: 'primary',
      startTime: '',
      endTime: '',
      notes: ''
    });
    setErrors({});
    setTouched({});
  };

  return (
    <div className="equipment-assignment-form">
      <div className="form-header">
        <h2>Assign Equipment to Batch</h2>
        <p className="form-subtitle">Link equipment to a production batch</p>
      </div>

      <Form onSubmit={handleSubmit}>
        <FormGroup 
          label="Batch" 
          required
          error={touched.batchId && errors.batchId}
        >
          <Select
            name="batchId"
            value={formData.batchId}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.batchId && errors.batchId}
          >
            <option value="">Select batch...</option>
            {batchList?.map(batch => (
              <option key={batch.id} value={batch.id}>
                {batch.batchCode} - {batch.status}
              </option>
            ))}
          </Select>
          {touched.batchId && errors.batchId && (
            <span className="error-message">{errors.batchId}</span>
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
                {equipment.name} - {equipment.type} ({equipment.status})
              </option>
            ))}
          </Select>
          {touched.equipmentId && errors.equipmentId && (
            <span className="error-message">{errors.equipmentId}</span>
          )}
        </FormGroup>

        <FormGroup label="Assignment Type">
          <Select
            name="assignmentType"
            value={formData.assignmentType}
            onChange={handleChange}
          >
            <option value="primary">Primary Equipment</option>
            <option value="secondary">Secondary Equipment</option>
            <option value="backup">Backup Equipment</option>
          </Select>
        </FormGroup>

        <FormGroup 
          label="Start Time" 
          required
          error={touched.startTime && errors.startTime}
        >
          <Input
            type="datetime-local"
            name="startTime"
            value={formData.startTime}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.startTime && errors.startTime}
          />
          {touched.startTime && errors.startTime && (
            <span className="error-message">{errors.startTime}</span>
          )}
        </FormGroup>

        <FormGroup 
          label="End Time"
          error={touched.endTime && errors.endTime}
        >
          <Input
            type="datetime-local"
            name="endTime"
            value={formData.endTime}
            onChange={handleChange}
            onBlur={handleBlur}
            error={touched.endTime && errors.endTime}
          />
          {touched.endTime && errors.endTime && (
            <span className="error-message">{errors.endTime}</span>
          )}
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
            Assign Equipment
          </button>
        </div>
      </Form>
    </div>
  );
};

export default EquipmentAssignmentForm;