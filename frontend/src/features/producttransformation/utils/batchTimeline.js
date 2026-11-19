/**
 * Product Transformation - batch timeline model
 *
 * Pure mapping from the batch-scoped API payloads to the seven traceability
 * steps. Kept outside the component so Fast Refresh stays happy and so the
 * mapping can be reasoned about (and tested) without rendering anything.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { CheckCircle, FileText, Flag, Flame, PackageCheck, Truck, Waves } from 'lucide-react';
import {
  formatDateTime,
  formatIsoDate,
  formatNumber,
  formatWeight,
  getComplianceColor,
  getQcResultColor,
  getStatusColor,
} from './format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';

// The batch lifecycle, in order. REJECTED is deliberately outside the chain.
export const STATUS_CHAIN = [
  'CREATED',
  'INTAKE',
  'WASHING',
  'DRYING',
  'PACKAGING',
  'COMPLETED',
  'SHIPPED',
];

// `/api/raw-intakes/batch/{id}` returns a single record (legacy contract) while
// the other five batch endpoints return arrays: normalise both shapes here so
// the timeline does not have to care which module it is reading.
const asArray = (value) => {
  if (Array.isArray(value)) return value;
  if (value && typeof value === 'object' && Object.keys(value).length > 0) return [value];
  return [];
};

const first = (list) => asArray(list)[0] ?? null;

const latestBy = (list, key) =>
  asArray(list).reduce((latest, item) => {
    if (!latest) return item;
    return new Date(item[key] ?? 0) >= new Date(latest[key] ?? 0) ? item : latest;
  }, null);

export const buildSteps = ({
  batch,
  intakes,
  washRecords,
  dryingRuns,
  qcCheckpoints,
  packagingRecords,
  complianceRecords,
}) => {
  const intake = first(intakes);
  const wash = first(washRecords);
  const drying = first(dryingRuns);
  const packaging = first(packagingRecords);
  const latestQc = latestBy(qcCheckpoints, 'checkpointTime');
  const latestCompliance = latestBy(complianceRecords, 'auditDate');

  const qcDetails = asArray(qcCheckpoints)
    .slice()
    .sort((a, b) => new Date(a.checkpointTime ?? 0) - new Date(b.checkpointTime ?? 0))
    .map((checkpoint) => ({
      label: `${formatEnumLabel(checkpoint.stage || 'UNKNOWN')} · ${formatDateTime(
        checkpoint.checkpointTime,
      )}`,
      value: `${checkpoint.result || 'UNKNOWN'}${
        checkpoint.defectsCount ? ` · ${checkpoint.defectsCount} defect(s)` : ''
      }`,
      badgeClass: getQcResultColor(checkpoint.result),
    }));

  const complianceDetails = asArray(complianceRecords).flatMap((record) => [
    {
      label: `${formatEnumLabel(record.complianceType || 'UNKNOWN')} · ${formatIsoDate(
        record.auditDate,
      )}`,
      value: record.result || 'UNKNOWN',
      badgeClass: getComplianceColor(record.result),
    },
    { label: 'Requirement', value: record.requirement || 'N/A' },
    {
      label: 'Next audit',
      value: record.nextAuditDate ? formatIsoDate(record.nextAuditDate) : 'Not scheduled',
    },
  ]);

  const statusIndex = STATUS_CHAIN.indexOf(batch?.currentStatus);

  return [
    {
      key: 'intake',
      title: 'Reception',
      icon: Truck,
      recorded: Boolean(intake),
      date: intake ? formatIsoDate(intake.intakeDate) : null,
      details: intake
        ? [
            { label: 'Received', value: formatWeight(intake.receivedQuantityKg) },
            { label: 'Grade', value: intake.receivedGrade ? `Grade ${intake.receivedGrade}` : 'N/A' },
            { label: 'Variety', value: intake.receivedVariety || 'N/A' },
            {
              label: 'Origin',
              value:
                [intake.sourceFarm, intake.sourceBlock].filter(Boolean).join(' / ') || 'N/A',
            },
            { label: 'Operator', value: intake.intakeOperator || 'N/A' },
            ...(asArray(intakes).length > 1
              ? [{ label: 'Receptions', value: `${asArray(intakes).length} records` }]
              : []),
          ]
        : [],
    },
    {
      key: 'washing',
      title: 'Washing & sorting',
      icon: Waves,
      recorded: Boolean(wash),
      date: wash ? formatDateTime(wash.startTime) : null,
      details: wash
        ? [
            { label: 'Input', value: formatWeight(wash.inputQuantityKg) },
            {
              label: 'Output',
              value: `${formatWeight(wash.outputQuantityKg)}${
                wash.yieldPercentage != null
                  ? ` · ${formatNumber(wash.yieldPercentage)} % yield`
                  : ''
              }`,
            },
            { label: 'Waste', value: formatWeight(wash.wasteQuantityKg) },
            { label: 'Water', value: `${formatNumber(wash.waterUsageLiters)} L` },
            { label: 'Equipment', value: wash.equipmentId || 'N/A' },
          ]
        : [],
    },
    {
      key: 'drying',
      title: 'Drying',
      icon: Flame,
      recorded: Boolean(drying),
      date: drying ? formatDateTime(drying.startTime) : null,
      details: drying
        ? [
            {
              label: 'Moisture',
              value: `${formatNumber(drying.startMoisturePct)} % → ${formatNumber(
                drying.endMoisturePct,
              )} %`,
            },
            {
              label: 'Temperature',
              value: `${formatNumber(drying.actualTemperatureC)} °C (target ${formatNumber(
                drying.targetTemperatureC,
              )} °C)`,
            },
            { label: 'Energy', value: `${formatNumber(drying.energyUsageKwh)} kWh` },
            { label: 'Duration', value: `${formatNumber(drying.durationHours)} h` },
            { label: 'Equipment', value: drying.equipmentId || 'N/A' },
          ]
        : [],
    },
    {
      key: 'qc',
      title: 'Quality control',
      icon: CheckCircle,
      recorded: qcDetails.length > 0,
      date: latestQc ? formatDateTime(latestQc.checkpointTime) : null,
      details: qcDetails,
    },
    {
      key: 'packaging',
      title: 'Packaging',
      icon: PackageCheck,
      recorded: Boolean(packaging),
      date: packaging ? formatIsoDate(packaging.packagingDate) : null,
      details: packaging
        ? [
            { label: 'Quantity', value: formatWeight(packaging.packageQuantityKg) },
            { label: 'Type', value: formatEnumLabel(packaging.packageType || 'UNKNOWN') },
            { label: 'Lot code', value: packaging.lotCode || 'N/A' },
            { label: 'Export ready', value: packaging.exportReady ? 'Yes' : 'No' },
            { label: 'Equipment', value: packaging.equipmentId || 'N/A' },
          ]
        : [],
    },
    {
      key: 'compliance',
      title: 'Compliance',
      icon: FileText,
      recorded: complianceDetails.length > 0,
      date: latestCompliance ? formatIsoDate(latestCompliance.auditDate) : null,
      details: complianceDetails,
    },
    {
      key: 'status',
      title: 'Batch status',
      icon: Flag,
      recorded: Boolean(batch),
      date: batch ? formatDateTime(batch.updatedAt) : null,
      details: batch
        ? [
            {
              label: 'Current status',
              value: batch.currentStatus || 'UNKNOWN',
              badgeClass: getStatusColor(batch.currentStatus),
            },
            {
              label: 'Progress',
              value:
                statusIndex >= 0
                  ? `step ${statusIndex + 1} of ${STATUS_CHAIN.length}`
                  : 'outside the standard chain',
            },
            {
              label: 'Harvest',
              value: `${formatIsoDate(batch.harvestDate)} · ${formatWeight(
                batch.harvestQuantityKg,
              )} · ${batch.mangoVariety || 'N/A'}`,
            },
            {
              label: 'Location',
              value: [batch.farmId, batch.blockId].filter(Boolean).join(' / ') || 'N/A',
            },
          ]
        : [],
    },
  ];
};
