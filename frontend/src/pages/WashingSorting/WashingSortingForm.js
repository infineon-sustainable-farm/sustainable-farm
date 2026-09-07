import React from 'react';
import { washingSortingApi } from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Form from '../../components/forms/Form';
import FormGroup from '../../components/forms/FormGroup';
import Input from '../../components/forms/Input';
import '../../components/forms/Form.css';

const INITIAL_FORM = {
  recordId: '',
  batchId: '',
  inputQuantityKg: '',
  outputQuantityKg: '',
  wasteQuantityKg: '',
  waterUsageLiters: '',
  startTime: '',
  endTime: '',
  equipmentId: '',
  operatorId: '',
};

const toDateTimeLocal = (val) => {
  if (!val) return '';
  const d = new Date(val);
  if (isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const WashingSortingForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        recordId: editRecord.recordId || '',
        batchId: editRecord.batchId || '',
        inputQuantityKg: editRecord.inputQuantityKg?.toString() || '',
        outputQuantityKg: editRecord.outputQuantityKg?.toString() || '',
        wasteQuantityKg: editRecord.wasteQuantityKg?.toString() || '',
        waterUsageLiters: editRecord.waterUsageLiters?.toString() || '',
        startTime: toDateTimeLocal(editRecord.startTime),
        endTime: toDateTimeLocal(editRecord.endTime),
        equipmentId: editRecord.equipmentId || '',
        operatorId: editRecord.operatorId || '',
      });
    } else {
      setFormData(INITIAL_FORM);
    }
    setErrors({});
    setSubmitError(null);
  }, [editRecord, isOpen]);

  const handleChange = (field) => (e) => {
    setFormData((prev) => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: null }));
  };

  const validate = () => {
    const newErrors = {};
    if (!isEdit && !formData.recordId.trim()) newErrors.recordId = 'Record ID is required';
    if (!isEdit && !formData.batchId.trim()) newErrors.batchId = 'Batch ID is required';
    if (!formData.inputQuantityKg || Number(formData.inputQuantityKg) <= 0) newErrors.inputQuantityKg = 'Must be > 0';
    if (!formData.outputQuantityKg || Number(formData.outputQuantityKg) <= 0) newErrors.outputQuantityKg = 'Must be > 0';
    if (formData.wasteQuantityKg === '' || Number(formData.wasteQuantityKg) < 0) newErrors.wasteQuantityKg = 'Must be ≥ 0';
    if (formData.waterUsageLiters === '' || Number(formData.waterUsageLiters) < 0) newErrors.waterUsageLiters = 'Must be ≥ 0';
    if (!formData.startTime) newErrors.startTime = 'Start time is required';
    if (!formData.endTime) newErrors.endTime = 'End time is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = {
        ...formData,
        inputQuantityKg: Number(formData.inputQuantityKg),
        outputQuantityKg: Number(formData.outputQuantityKg),
        wasteQuantityKg: Number(formData.wasteQuantityKg),
        waterUsageLiters: Number(formData.waterUsageLiters),
        startTime: formData.startTime ? `${formData.startTime}:00` : null,
        endTime: formData.endTime ? `${formData.endTime}:00` : null,
      };
      if (!payload.equipmentId) delete payload.equipmentId;
      if (!payload.operatorId) delete payload.operatorId;
      if (isEdit) {
        await washingSortingApi.update(editRecord.recordId, payload);
      } else {
        await washingSortingApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save wash/sort record';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save wash/sort record');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Wash/Sort Record' : 'New Wash/Sort Record'} size="lg">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

        <FormGroup label="Record ID" required={!isEdit}>
          <Input
            value={formData.recordId}
            onChange={handleChange('recordId')}
            disabled={isEdit}
            placeholder="e.g. WSH-2024-004"
            error={!!errors.recordId}
          />
          {errors.recordId && <span className="form-error">{errors.recordId}</span>}
        </FormGroup>

        <FormGroup label="Batch ID" required={!isEdit}>
          <Input
            value={formData.batchId}
            onChange={handleChange('batchId')}
            disabled={isEdit}
            placeholder="e.g. BATCH-2024-006"
            error={!!errors.batchId}
          />
          {errors.batchId && <span className="form-error">{errors.batchId}</span>}
        </FormGroup>

        <FormGroup label="Input Quantity (kg)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.inputQuantityKg}
            onChange={handleChange('inputQuantityKg')}
            error={!!errors.inputQuantityKg}
          />
          {errors.inputQuantityKg && <span className="form-error">{errors.inputQuantityKg}</span>}
        </FormGroup>

        <FormGroup label="Output Quantity (kg)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.outputQuantityKg}
            onChange={handleChange('outputQuantityKg')}
            error={!!errors.outputQuantityKg}
          />
          {errors.outputQuantityKg && <span className="form-error">{errors.outputQuantityKg}</span>}
        </FormGroup>

        <FormGroup label="Waste Quantity (kg)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.wasteQuantityKg}
            onChange={handleChange('wasteQuantityKg')}
            error={!!errors.wasteQuantityKg}
          />
          {errors.wasteQuantityKg && <span className="form-error">{errors.wasteQuantityKg}</span>}
        </FormGroup>

        <FormGroup label="Water Usage (L)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.waterUsageLiters}
            onChange={handleChange('waterUsageLiters')}
            error={!!errors.waterUsageLiters}
          />
          {errors.waterUsageLiters && <span className="form-error">{errors.waterUsageLiters}</span>}
        </FormGroup>

        <FormGroup label="Start Time" required>
          <Input
            type="datetime-local"
            value={formData.startTime}
            onChange={handleChange('startTime')}
            error={!!errors.startTime}
          />
          {errors.startTime && <span className="form-error">{errors.startTime}</span>}
        </FormGroup>

        <FormGroup label="End Time" required>
          <Input
            type="datetime-local"
            value={formData.endTime}
            onChange={handleChange('endTime')}
            error={!!errors.endTime}
          />
          {errors.endTime && <span className="form-error">{errors.endTime}</span>}
        </FormGroup>

        <FormGroup label="Equipment ID">
          <Input
            value={formData.equipmentId}
            onChange={handleChange('equipmentId')}
            placeholder="e.g. EQ-001 (optional)"
          />
        </FormGroup>

        <FormGroup label="Operator ID">
          <Input
            value={formData.operatorId}
            onChange={handleChange('operatorId')}
            placeholder="e.g. OP-001 (optional)"
          />
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Record' : 'Create Record'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default WashingSortingForm;