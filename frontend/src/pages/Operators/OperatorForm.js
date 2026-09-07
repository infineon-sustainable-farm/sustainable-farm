import React from 'react';
import { operatorApi } from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Form from '../../components/forms/Form';
import FormGroup from '../../components/forms/FormGroup';
import Input from '../../components/forms/Input';
import Select from '../../components/forms/Select';
import '../../components/forms/Form.css';

const ROLES = [
  { value: 'WASHER', label: 'Washer' },
  { value: 'DRYER', label: 'Dryer' },
  { value: 'PACKAGER', label: 'Packager' },
  { value: 'QC_INSPECTOR', label: 'QC Inspector' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'AUDITOR', label: 'Auditor' },
];

const ACTIVE_STATUSES = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

const INITIAL_FORM = {
  operatorId: '',
  operatorName: '',
  role: '',
  certifications: '',
  activeStatus: 'ACTIVE',
  hireDate: '',
};

const OperatorForm = ({ isOpen, onClose, onSuccess, editRecord = null }) => {
  const isEdit = !!editRecord;
  const [formData, setFormData] = React.useState(INITIAL_FORM);
  const [errors, setErrors] = React.useState({});
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState(null);

  React.useEffect(() => {
    if (editRecord) {
      setFormData({
        operatorId: editRecord.operatorId || '',
        operatorName: editRecord.operatorName || '',
        role: editRecord.role || '',
        certifications: editRecord.certifications || '',
        activeStatus: editRecord.activeStatus || 'ACTIVE',
        hireDate: editRecord.hireDate || '',
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
    if (!isEdit && !formData.operatorId.trim()) newErrors.operatorId = 'Operator ID is required';
    if (!formData.operatorName.trim()) newErrors.operatorName = 'Name is required';
    if (!formData.role) newErrors.role = 'Role is required';
    if (!formData.hireDate) newErrors.hireDate = 'Hire date is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const payload = { ...formData };
      if (isEdit) {
        await operatorApi.update(editRecord.operatorId, payload);
      } else {
        await operatorApi.create(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      const message = err.response?.data?.message || err.response?.data || 'Failed to save operator';
      setSubmitError(typeof message === 'string' ? message : 'Failed to save operator');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Edit Operator' : 'New Operator'} size="md">
      <Form onSubmit={handleSubmit}>
        {submitError && <div className="form-submit-error">{submitError}</div>}

        <FormGroup label="Operator ID" required={!isEdit}>
          <Input
            value={formData.operatorId}
            onChange={handleChange('operatorId')}
            disabled={isEdit}
            placeholder="e.g. OP-011"
            error={!!errors.operatorId}
          />
          {errors.operatorId && <span className="form-error">{errors.operatorId}</span>}
        </FormGroup>

        <FormGroup label="Operator Name" required>
          <Input
            value={formData.operatorName}
            onChange={handleChange('operatorName')}
            placeholder="e.g. Jean Kaboré"
            error={!!errors.operatorName}
          />
          {errors.operatorName && <span className="form-error">{errors.operatorName}</span>}
        </FormGroup>

        <FormGroup label="Role" required>
          <Select
            value={formData.role}
            onChange={handleChange('role')}
            options={ROLES}
            placeholder="Select role"
            error={!!errors.role}
          />
          {errors.role && <span className="form-error">{errors.role}</span>}
        </FormGroup>

        <FormGroup label="Certifications">
          <Input
            value={formData.certifications}
            onChange={handleChange('certifications')}
            placeholder="e.g. HACCP, Food Safety Level 2"
          />
        </FormGroup>

        <FormGroup label="Active Status">
          <Select
            value={formData.activeStatus}
            onChange={handleChange('activeStatus')}
            options={ACTIVE_STATUSES}
          />
        </FormGroup>

        <FormGroup label="Hire Date" required>
          <Input
            type="date"
            value={formData.hireDate}
            onChange={handleChange('hireDate')}
            error={!!errors.hireDate}
          />
          {errors.hireDate && <span className="form-error">{errors.hireDate}</span>}
        </FormGroup>

        <div className="form-actions">
          <Button variant="secondary" onClick={onClose} disabled={submitting} type="button">
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting} type="button">
            {submitting ? 'Saving...' : isEdit ? 'Update Operator' : 'Create Operator'}
          </Button>
        </div>
      </Form>
    </Modal>
  );
};

export default OperatorForm;