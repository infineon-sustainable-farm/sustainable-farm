import React from 'react';
import { rawIntakeApi } from '../../services/api';
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

const QUALITY_GRADES = [
  { value: 'A', label: 'Grade A — Premium' },
  { value: 'B', label: 'Grade B — Standard' },
  { value: 'C', label: 'Grade C — Commercial' },
  { value: 'D', label: 'Grade D — Processing' },
];

const INITIAL_FORM = {
  intakeId: '',
  batchId: '',
  sourceFarm: '',
  sourceBlock: '',
  intakeDate: '',
  receivedQuantityKg: '',
  receivedVariety: '',
  receivedGrade: '',
  intakeOperator: '',
};

const RawIntakeForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        intakeId: editRecord.intakeId || '',
        batchId: editRecord.batchId || '',
        sourceFarm: editRecord.sourceFarm || '',
        sourceBlock: editRecord.sourceBlock || '',
        intakeDate: editRecord.intakeDate || '',
        receivedQuantityKg: editRecord.receivedQuantityKg?.toString() || '',
        receivedVariety: editRecord.receivedVariety || '',
        receivedGrade: editRecord.receivedGrade || '',
        intakeOperator: editRecord.intakeOperator || '',
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
      if (!formData.intakeId.trim()) newErrors.intakeId = 'Intake ID is required';
      if (!formData.batchId.trim()) newErrors.batchId = 'Batch ID is required';
    }
    if (!formData.sourceFarm.trim()) newErrors.sourceFarm = 'Source farm is required';
    if (!formData.sourceBlock.trim()) newErrors.sourceBlock = 'Source block is required';
    if (!formData.intakeDate) newErrors.intakeDate = 'Intake date is required';
    if (!formData.receivedQuantityKg || Number(formData.receivedQuantityKg) <= 0) {
      newErrors.receivedQuantityKg = 'Quantity must be > 0';
    }
    if (!formData.receivedVariety) newErrors.receivedVariety = 'Variety is required';
    if (!formData.receivedGrade) newErrors.receivedGrade = 'Grade is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = { ...formData };
      if (payload.receivedQuantityKg) {
        payload.receivedQuantityKg = Number(payload.receivedQuantityKg);
      }
      if (isEdit) {
        await rawIntakeApi.update(editRecord.intakeId, payload);
      } else {
        await rawIntakeApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save raw intake';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save raw intake');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Raw Intake' : 'New Raw Intake'} size="lg">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

        <FormGroup label="Intake ID" required={!isEdit}>
          <Input
            value={formData.intakeId}
            onChange={handleChange('intakeId')}
            disabled={isEdit}
            placeholder="e.g. INTAKE-2024-006"
            error={!!errors.intakeId}
          />
          {errors.intakeId && <span className="form-error">{errors.intakeId}</span>}
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

        <FormGroup label="Source Farm" required>
          <Input
            value={formData.sourceFarm}
            onChange={handleChange('sourceFarm')}
            placeholder="e.g. FARM-001"
            error={!!errors.sourceFarm}
          />
          {errors.sourceFarm && <span className="form-error">{errors.sourceFarm}</span>}
        </FormGroup>

        <FormGroup label="Source Block" required>
          <Input
            value={formData.sourceBlock}
            onChange={handleChange('sourceBlock')}
            placeholder="e.g. BLOCK-A"
            error={!!errors.sourceBlock}
          />
          {errors.sourceBlock && <span className="form-error">{errors.sourceBlock}</span>}
        </FormGroup>

        <FormGroup label="Intake Date" required>
          <Input
            type="date"
            value={formData.intakeDate}
            onChange={handleChange('intakeDate')}
            error={!!errors.intakeDate}
          />
          {errors.intakeDate && <span className="form-error">{errors.intakeDate}</span>}
        </FormGroup>

        <FormGroup label="Received Quantity (kg)" required>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={formData.receivedQuantityKg}
            onChange={handleChange('receivedQuantityKg')}
            placeholder="e.g. 1250.00"
            error={!!errors.receivedQuantityKg}
          />
          {errors.receivedQuantityKg && <span className="form-error">{errors.receivedQuantityKg}</span>}
        </FormGroup>

        <FormGroup label="Received Variety" required>
          <Select
            value={formData.receivedVariety}
            onChange={handleChange('receivedVariety')}
            options={MANGO_VARIETIES}
            placeholder="Select variety"
            error={!!errors.receivedVariety}
          />
          {errors.receivedVariety && <span className="form-error">{errors.receivedVariety}</span>}
        </FormGroup>

        <FormGroup label="Received Grade" required>
          <Select
            value={formData.receivedGrade}
            onChange={handleChange('receivedGrade')}
            options={QUALITY_GRADES}
            placeholder="Select grade"
            error={!!errors.receivedGrade}
          />
          {errors.receivedGrade && <span className="form-error">{errors.receivedGrade}</span>}
        </FormGroup>

        <FormGroup label="Intake Operator">
          <Input
            value={formData.intakeOperator}
            onChange={handleChange('intakeOperator')}
            placeholder="e.g. OP-001"
          />
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Intake' : 'Create Intake'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default RawIntakeForm;