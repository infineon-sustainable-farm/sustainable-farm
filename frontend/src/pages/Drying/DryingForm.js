import React from 'react';
import { dryingApi } from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Form from '../../components/forms/Form';
import FormGroup from '../../components/forms/FormGroup';
import Input from '../../components/forms/Input';
import '../../components/forms/Form.css';

const INITIAL_FORM = {
  runId: '',
  batchId: '',
  durationHours: '',
  targetTemperatureC: '',
  actualTemperatureC: '',
  startMoisturePct: '',
  endMoisturePct: '',
  energyUsageKwh: '',
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

const DryingForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        runId: editRecord.runId || '',
        batchId: editRecord.batchId || '',
        durationHours: editRecord.durationHours?.toString() || '',
        targetTemperatureC: editRecord.targetTemperatureC?.toString() || '',
        actualTemperatureC: editRecord.actualTemperatureC?.toString() || '',
        startMoisturePct: editRecord.startMoisturePct?.toString() || '',
        endMoisturePct: editRecord.endMoisturePct?.toString() || '',
        energyUsageKwh: editRecord.energyUsageKwh?.toString() || '',
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
    if (!isEdit && !formData.runId.trim()) newErrors.runId = 'Run ID is required';
    if (!isEdit && !formData.batchId.trim()) newErrors.batchId = 'Batch ID is required';
    if (!formData.durationHours || Number(formData.durationHours) <= 0) newErrors.durationHours = 'Must be > 0';
    if (!formData.targetTemperatureC) newErrors.targetTemperatureC = 'Required';
    if (!formData.actualTemperatureC) newErrors.actualTemperatureC = 'Required';
    if (!formData.startMoisturePct || Number(formData.startMoisturePct) <= 0) newErrors.startMoisturePct = 'Must be > 0';
    if (!formData.endMoisturePct) newErrors.endMoisturePct = 'Required';
    else {
      const end = Number(formData.endMoisturePct);
      if (end < 6 || end > 18) newErrors.endMoisturePct = 'Must be 6–18% (EU compliance)';
    }
    if (formData.energyUsageKwh === '' || Number(formData.energyUsageKwh) < 0) newErrors.energyUsageKwh = 'Must be ≥ 0';
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
        durationHours: Number(formData.durationHours),
        targetTemperatureC: Number(formData.targetTemperatureC),
        actualTemperatureC: Number(formData.actualTemperatureC),
        startMoisturePct: Number(formData.startMoisturePct),
        endMoisturePct: Number(formData.endMoisturePct),
        energyUsageKwh: Number(formData.energyUsageKwh),
        startTime: formData.startTime ? `${formData.startTime}:00` : null,
        endTime: formData.endTime ? `${formData.endTime}:00` : null,
      };
      if (!payload.equipmentId) delete payload.equipmentId;
      if (!payload.operatorId) delete payload.operatorId;
      if (isEdit) {
        await dryingApi.update(editRecord.runId, payload);
      } else {
        await dryingApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save drying run';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save drying run');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Drying Run' : 'New Drying Run'} size="lg">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

        <FormGroup label="Run ID" required={!isEdit}>
          <Input
            value={formData.runId}
            onChange={handleChange('runId')}
            disabled={isEdit}
            placeholder="e.g. DRY-2024-003"
            error={!!errors.runId}
          />
          {errors.runId && <span className="form-error">{errors.runId}</span>}
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

        <FormGroup label="Duration (hours)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.durationHours}
            onChange={handleChange('durationHours')}
            error={!!errors.durationHours}
          />
          {errors.durationHours && <span className="form-error">{errors.durationHours}</span>}
        </FormGroup>

        <FormGroup label="Target Temp (°C)" required>
          <Input
            type="number"
            step="0.01"
            value={formData.targetTemperatureC}
            onChange={handleChange('targetTemperatureC')}
            error={!!errors.targetTemperatureC}
          />
          {errors.targetTemperatureC && <span className="form-error">{errors.targetTemperatureC}</span>}
        </FormGroup>

        <FormGroup label="Actual Temp (°C)" required>
          <Input
            type="number"
            step="0.01"
            value={formData.actualTemperatureC}
            onChange={handleChange('actualTemperatureC')}
            error={!!errors.actualTemperatureC}
          />
          {errors.actualTemperatureC && <span className="form-error">{errors.actualTemperatureC}</span>}
        </FormGroup>

        <FormGroup label="Start Moisture (%)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.startMoisturePct}
            onChange={handleChange('startMoisturePct')}
            error={!!errors.startMoisturePct}
          />
          {errors.startMoisturePct && <span className="form-error">{errors.startMoisturePct}</span>}
        </FormGroup>

        <FormGroup label="End Moisture (%) — EU: 6–18%" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.endMoisturePct}
            onChange={handleChange('endMoisturePct')}
            error={!!errors.endMoisturePct}
          />
          {errors.endMoisturePct && <span className="form-error">{errors.endMoisturePct}</span>}
        </FormGroup>

        <FormGroup label="Energy Usage (kWh)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.energyUsageKwh}
            onChange={handleChange('energyUsageKwh')}
            error={!!errors.energyUsageKwh}
          />
          {errors.energyUsageKwh && <span className="form-error">{errors.energyUsageKwh}</span>}
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
            placeholder="e.g. EQ-003 (optional)"
          />
        </FormGroup>

        <FormGroup label="Operator ID">
          <Input
            value={formData.operatorId}
            onChange={handleChange('operatorId')}
            placeholder="e.g. OP-003 (optional)"
          />
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Run' : 'Create Run'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default DryingForm;