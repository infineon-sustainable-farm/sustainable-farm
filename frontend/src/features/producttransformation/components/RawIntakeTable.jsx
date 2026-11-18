import { useTranslation } from 'react-i18next';
import { Badge, Table } from '../../../shared/ui';
import { formatEnum, formatIsoDate, formatWeight, notAvailable } from '../utils/format';
import RowActions from './RowActions';

/** Table of the raw intakes, with Edit and Delete on each row. */
export default function RawIntakeTable({ intakes = [], onEdit, onDelete, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'intakeId', header: t('rawIntake.columns.intake'), className: 'whitespace-nowrap' },
    { key: 'batchId', header: t('rawIntake.columns.batch'), className: 'whitespace-nowrap' },
    {
      key: 'intakeDate',
      header: t('rawIntake.columns.date'),
      className: 'whitespace-nowrap',
      cell: (row) => formatIsoDate(row.intakeDate),
    },
    {
      key: 'source',
      header: t('rawIntake.columns.source'),
      className: 'whitespace-nowrap',
      cell: (row) => t('format.source', { farm: row.sourceFarm, block: row.sourceBlock }),
    },
    {
      key: 'quantity',
      header: t('rawIntake.columns.quantity'),
      className: 'whitespace-nowrap',
      cell: (row) => formatWeight(row.receivedQuantityKg),
    },
    {
      key: 'variety',
      header: t('rawIntake.columns.variety'),
      cell: (row) => formatEnum('variety', row.receivedVariety),
    },
    {
      key: 'grade',
      header: t('rawIntake.columns.grade'),
      cell: (row) => <Badge>{t('enums.grade', { grade: row.receivedGrade || notAvailable() })}</Badge>,
    },
    {
      key: 'operator',
      header: t('rawIntake.columns.operator'),
      cell: (row) => row.intakeOperator || notAvailable(),
    },
  ];

  return (
    <Table
      caption={t('rawIntake.title')}
      columns={columns}
      rows={intakes}
      getRowKey={(row) => row.intakeId}
      loading={loading}
      rowActions={(intake) => <RowActions onEdit={() => onEdit(intake)} onDelete={() => onDelete(intake)} />}
    />
  );
}
