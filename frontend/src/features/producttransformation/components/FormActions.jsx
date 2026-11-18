import { useTranslation } from 'react-i18next';
import { Button } from '../../../shared/ui';

/**
 * Cancel and submit at the bottom of a Product Transformation form. The submit button reads
 * "Create" for a new record, "Save" for an edit and "Saving..." while the request runs.
 */
export default function FormActions({ onCancel, saving = false, editing = false, submitLabel }) {
  const { t } = useTranslation('producttransformation');
  return (
    <div className="flex justify-end gap-3 pt-2">
      <Button variant="secondary" onClick={onCancel} disabled={saving}>
        {t('common.cancel')}
      </Button>
      <Button type="submit" disabled={saving}>
        {saving ? t('common.saving') : (submitLabel ?? (editing ? t('common.save') : t('common.create')))}
      </Button>
    </div>
  );
}
