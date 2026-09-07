import React from 'react';
import { harvestApi } from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Form from '../../components/forms/Form';
import FormGroup from '../../components/forms/FormGroup';
import Input from '../../components/forms/Input';
import Select from '../../components/forms/Select';
import './HarvestForm.css';

const MANGO_VARIETIES = [
  { value: 'KEITT', label: 'Keitt' },
  { value: 'KENT', label: 'Kent' },
  { value: 'TOMMY', label: 'Tommy Atkins' },
  { value: 'AMELIE', label: 'Amélie' },
  { value: 'OTHER', label: 'Other' },
];

const QUALITY_GRADES = [
  { value: 'A', label: 'Grade A — Premium' },
  { value: 'B', label: 'Grade B — Standard' },
  { value: 'C', label: 'Grade C — Commercial' },
  { value: 'D', label: 'Grade D — Processing' },
];

const INITIAL_FORM = {
  harvestId: '',
  batchId: '',
  harvestDate: '',
  harvestTime: '',
  mangoVariety: '',
  farmId: '',
  blockId: '',
  harvestQuantityKg: '',
  qualityGrade: '',
  qualityGradeDescription: '',
  harvestTeamId: '',
  harvestSupervisor: '',
  weatherConditions: '',
  storageLocation: '',
};

const HarvestForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        harvestId: editRecord.harvestId || '',
        batchId: editRecord.batchId || '',
        harvestDate: editRecord.harvestDate || '',
        harvestTime: editRecord.harvestTime || '',
        mangoVariety: editRecord.mangoVariety || '',
        farmId: editRecord.farmId || '',
        blockId: editRecord.blockId || '',
        harvestQuantityKg: editRecord.harvestQuantityKg?.toString() || '',
        qualityGrade: editRecord.qualityGrade || '',
        qualityGradeDescription: editRecord.qualityGradeDescription || '',
        harvestTeamId: editRecord.harvestTeamId || '',
        harvestSupervisor: editRecord.harvestSupervisor || '',
        weatherConditions: editRecord.weatherConditions || '',
        storageLocation: editRecord.storageLocation || '',
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
    if (!isEdit) {
      if (!formData.harvestId.trim()) newErrors.harvestId = 'Harvest ID is required';
      if (!formData.batchId.trim()) newErrors.batchId = 'Batch ID is required';
    }
    if (!formData.harvestDate) newErrors.harvestDate = 'Harvest date is required';
    if (!formData.mangoVariety) newErrors.mangoVariety = 'Variety is required';
    if (!formData.farmId.trim()) newErrors.farmId = 'Farm ID is required';
    if (!formData.blockId.trim()) newErrors.blockId = 'Block ID is required';
    if (!formData.harvestQuantityKg || Number(formData.harvestQuantityKg) <= 0) {
      newErrors.harvestQuantityKg = 'Quantity must be > 0';
    }
    if (!formData.qualityGrade) newErrors.qualityGrade = 'Quality grade is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = { ...formData };
      if (!payload.harvestTime) delete payload.harvestTime;
      if (payload.harvestQuantityKg) {
        payload.harvestQuantityKg = Number(payload.harvestQuantityKg);
      }
      if (isEdit) {
        await harvestApi.update(editRecord.harvestId, payload);
      } else {
        await harvestApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save harvest event';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save harvest event');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Harvest Event' : 'New Harvest Event'} size="lg">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

        <FormGroup label="Harvest ID" required={!isEdit}>
          <Input
            value={formData.harvestId}
            onChange={handleChange('harvestId')}
            disabled={isEdit}
            placeholder="e.g. HVST-2024-006"
            error={!!errors.harvestId}
          />
          {errors.harvestId && <span className="form-error">{errors.harvestId}</span>}
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

        <FormGroup label="Harvest Date" required>
          <Input
            type="date"
            value={formData.harvestDate}
            onChange={handleChange('harvestDate')}
            error={!!errors.harvestDate}
          />
          {errors.harvestDate && <span className="form-error">{errors.harvestDate}</span>}
        </FormGroup>

        <FormGroup label="Harvest Time">
          <Input
            type="time"
            value={formData.harvestTime}
            onChange={handleChange('harvestTime')}
          />
        </FormGroup>

        <FormGroup label="Mango Variety" required>
          <Select
            value={formData.mangoVariety}
            onChange={handleChange('mangoVariety')}
            options={MANGO_VARIETIES}
            placeholder="Select variety"
            error={!!errors.mangoVariety}
          />
          {errors.mangoVariety && <span className="form-error">{errors.mangoVariety}</span>}
        </FormGroup>

        <FormGroup label="Farm ID" required>
          <Input
            value={formData.farmId}
            onChange={handleChange('farmId')}
            placeholder="e.g. FARM-001"
            error={!!errors.farmId}
          />
          {errors.farmId && <span className="form-error">{errors.farmId}</span>}
        </FormGroup>

        <FormGroup label="Block ID" required>
          <Input
            value={formData.blockId}
            onChange={handleChange('blockId')}
            placeholder="e.g. BLOCK-A"
            error={!!errors.blockId}
          />
          {errors.blockId && <span className="form-error">{errors.blockId}</span>}
        </FormGroup>

        <FormGroup label="Quantity (kg)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.harvestQuantityKg}
            onChange={handleChange('harvestQuantityKg')}
            placeholder="e.g. 1250.00"
            error={!!errors.harvestQuantityKg}
          />
          {errors.harvestQuantityKg && <span className="form-error">{errors.harvestQuantityKg}</span>}
        </FormGroup>

        <FormGroup label="Quality Grade" required>
          <Select
            value={formData.qualityGrade}
            onChange={handleChange('qualityGrade')}
            options={QUALITY_GRADES}
            placeholder="Select grade"
            error={!!errors.qualityGrade}
          />
          {errors.qualityGrade && <span className="form-error">{errors.qualityGrade}</span>}
        </FormGroup>

        <FormGroup label="Grade Description">
          <Input
            value={formData.qualityGradeDescription}
            onChange={handleChange('qualityGradeDescription')}
            placeholder="Optional description"
          />
        </FormGroup>

        <FormGroup label="Harvest Team ID">
          <Input
            value={formData.harvestTeamId}
            onChange={handleChange('harvestTeamId')}
            placeholder="e.g. TEAM-ALPHA"
          />
        </FormGroup>

        <FormGroup label="Supervisor">
          <Input
            value={formData.harvestSupervisor}
            onChange={handleChange('harvestSupervisor')}
            placeholder="Supervisor name"
          />
        </FormGroup>

        <FormGroup label="Weather Conditions">
          <Input
            value={formData.weatherConditions}
            onChange={handleChange('weatherConditions')}
            placeholder="e.g. Sunny, Cloudy"
          />
        </FormGroup>

        <FormGroup label="Storage Location">
          <Input
            value={formData.storageLocation}
            onChange={handleChange('storageLocation')}
            placeholder="e.g. STORAGE-A"
          />
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Harvest' : 'Create Harvest'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default HarvestForm;