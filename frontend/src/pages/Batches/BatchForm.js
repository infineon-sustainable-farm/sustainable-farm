import React from 'react';
import { batchApi } from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Form from '../../components/forms/Form';
import FormGroup from '../../components/forms/FormGroup';
import Input from '../../components/forms/Input';
import Select from '../../components/forms/Select';
import '../../components/forms/Form.css';

const MANGO_VARIETIES = [
  { value: 'KEITT', label: 'Keitt' },
  { value: 'KENT', label: 'Kent' },
  { value: 'TOMMY', label: 'Tommy Atkins' },
  { value: 'AMELIE', label: 'Amélie' },
  { value: 'OTHER', label: 'Other' },
];

const BATCH_STATUSES = [
  { value: 'CREATED', label: 'Created' },
  { value: 'INTAKE', label: 'Intake' },
  { value: 'WASHING', label: 'Washing' },
  { value: 'DRYING', label: 'Drying' },
  { value: 'PACKAGING', label: 'Packaging' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'REJECTED', label: 'Rejected' },
];

const INITIAL_FORM = {
  batchId: '',
  harvestDate: '',
  mangoVariety: '',
  harvestQuantityKg: '',
  farmId: '',
  blockId: '',
  currentStatus: '',
};

const BatchForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        batchId: editRecord.batchId || '',
        harvestDate: editRecord.harvestDate || '',
        mangoVariety: editRecord.mangoVariety || '',
        harvestQuantityKg: editRecord.harvestQuantityKg?.toString() || '',
        farmId: editRecord.farmId || '',
        blockId: editRecord.blockId || '',
        currentStatus: editRecord.currentStatus || '',
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
    if (!isEdit && !formData.batchId.trim()) newErrors.batchId = 'Batch ID is required';
    if (!formData.harvestDate) newErrors.harvestDate = 'Harvest date is required';
    if (!formData.mangoVariety) newErrors.mangoVariety = 'Variety is required';
    if (!formData.harvestQuantityKg || Number(formData.harvestQuantityKg) <= 0) {
      newErrors.harvestQuantityKg = 'Quantity must be > 0';
    }
    if (!formData.farmId.trim()) newErrors.farmId = 'Farm ID is required';
    if (!formData.blockId.trim()) newErrors.blockId = 'Block ID is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = { ...formData };
      if (payload.harvestQuantityKg) {
        payload.harvestQuantityKg = Number(payload.harvestQuantityKg);
      }
      if (isEdit) {
        await batchApi.update(editRecord.batchId, payload);
      } else {
        await batchApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save batch';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save batch');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Batch' : 'New Batch'} size="md">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

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

        <FormGroup label="Status">
          <Select
            value={formData.currentStatus}
            onChange={handleChange('currentStatus')}
            options={BATCH_STATUSES}
            placeholder="Select status (optional)"
          />
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Batch' : 'Create Batch'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default BatchForm;