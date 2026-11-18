import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';
import { QC_RESULTS, QC_STAGES, qcResultLabel, qcStageLabel } from './qcOptions';

/** Batch, stage, result, inspector and date filters of the QC checkpoint list. */
export default function QcCheckpointFilters({ filters, onChange, batches, inspectors, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={t('qc.count', { count })}>
      <Field label={t('qc.batch')}>
        <Select aria-label={t('qc.filterBatch')} value={filters.batchId} onChange={update('batchId')}>
          <option value="all">{t('qc.allBatches')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('qc.stage')}>
        <Select aria-label={t('qc.filterStage')} value={filters.stage} onChange={update('stage')}>
          <option value="all">{t('qc.allStages')}</option>
          {QC_STAGES.map((stage) => (
            <option key={stage} value={stage}>
              {qcStageLabel(stage)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('qc.result')}>
        <Select aria-label={t('qc.filterResult')} value={filters.result} onChange={update('result')}>
          <option value="all">{t('qc.allResults')}</option>
          {QC_RESULTS.map((result) => (
            <option key={result} value={result}>
              {qcResultLabel(result)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('qc.inspector')}>
        <Select aria-label={t('qc.filterInspector')} value={filters.inspectorId} onChange={update('inspectorId')}>
          <option value="all">{t('qc.allInspectors')}</option>
          {inspectors.map((operator) => (
            <option key={operator.operatorId} value={operator.operatorId}>
              {operator.operatorName}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('qc.date')}>
        <Input type="date" aria-label={t('qc.filterDate')} value={filters.date} onChange={update('date')} />
      </Field>
    </FilterBar>
  );
}
