import { Pencil, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IconButton } from '../../../shared/ui';

/** Edit and Delete icon buttons of a table row. */
export default function RowActions({ onEdit, onDelete, children }) {
  const { t } = useTranslation('producttransformation');
  return (
    <>
      {children}
      {onEdit && <IconButton icon={Pencil} label={t('common.edit')} onClick={onEdit} />}
      {onDelete && <IconButton icon={Trash2} label={t('common.delete')} tone="danger" onClick={onDelete} />}
    </>
  );
}
