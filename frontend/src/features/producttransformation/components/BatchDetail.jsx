/**
 * Product Transformation - Batch traceability timeline
 *
 * Read-only page per batch, from reception to shipment. The data is assembled
 * from the batch-scoped endpoint of each PT module so no new backend route is
 * needed and the page always reflects the records themselves.
 *
 * A step with no record is shown as "Not recorded yet" rather than as an error:
 * a batch created five minutes ago legitimately has six empty steps.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { batchApi } from '../api/batchApi';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { washSortApi } from '../api/washSortApi';
import { dryingApi } from '../api/dryingApi';
import { qcApi } from '../api/qcApi';
import { packagingApi } from '../api/packagingApi';
import { complianceApi } from '../api/complianceApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import { formatWeight, getStatusColor } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { STATUS_CHAIN, buildSteps } from '../utils/batchTimeline';

import { CARD, PAGE_TITLE, SECTION_CARD } from '../styles/ptStyles';

// `/api/raw-intakes/batch/{id}` answers 404 when the batch has no reception yet,
// while the other five modules answer with an empty array. A missing reception is
// a legitimate timeline state, not a failure; anything else still surfaces.
const getIntakesOrEmpty = (batchId) =>
  rawIntakeApi.getRawIntakesByBatch(batchId).catch((err) => {
    if (err?.status === 404) return null;
    throw err;
  });

const StepDetails = ({ details }) => (
  <dl className="mt-2 space-y-1">
    {details.map((detail, index) => (
      <div key={`${detail.label}-${index}`} className="flex flex-wrap items-baseline gap-x-2 text-sm">
        <dt className="text-gray-500">{detail.label}</dt>
        {detail.badgeClass ? (
          <dd>
            <span
              className={`px-2 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full ${detail.badgeClass}`}
            >
              {detail.value}
            </span>
          </dd>
        ) : (
          <dd className="text-gray-900">{detail.value}</dd>
        )}
      </div>
    ))}
  </dl>
);

const BatchDetail = () => {
  const { batchId } = useParams();
  const [batch, setBatch] = useState(null);
  const [steps, setSteps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [batchData, intakes, washRecords, dryingRuns, qcCheckpoints, packagingRecords, complianceRecords] =
        await Promise.all([
          batchApi.getBatchById(batchId),
          getIntakesOrEmpty(batchId),
          washSortApi.getWashSortRecordsByBatch(batchId),
          dryingApi.getDryingRunsByBatch(batchId),
          qcApi.getQcCheckpointsByBatch(batchId),
          packagingApi.getPackagingRecordsByBatch(batchId),
          complianceApi.getComplianceRecordsByBatch(batchId),
        ]);

      setBatch(batchData);
      setSteps(
        buildSteps({
          batch: batchData,
          intakes,
          washRecords,
          dryingRuns,
          qcCheckpoints,
          packagingRecords,
          complianceRecords,
        }),
      );
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [batchId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const backLink = (
    <Link
      to="/product-transformation/batches"
      className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
    >
      <ArrowLeft className="w-4 h-4" aria-hidden="true" />
      Back to batches
    </Link>
  );

  if (loading) return <LoadingState message="Loading batch timeline..." />;

  if (error) {
    return (
      <div className="p-6">
        {backLink}
        <div className="mt-4">
          <ErrorState message={error?.message || `Failed to load batch ${batchId}`} onRetry={load} />
        </div>
      </div>
    );
  }

  const currentStatus = batch?.currentStatus;
  const currentIndex = STATUS_CHAIN.indexOf(currentStatus);
  const recordedSteps = steps.filter((step) => step.recorded).length;

  return (
    <div className="p-6">
      {backLink}

      <header className={`mt-4 ${SECTION_CARD}`}>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold text-gray-500">
              Traceability timeline
            </p>
            <h1 className={PAGE_TITLE}>{batch?.batchId || batchId}</h1>
            <p className="mt-1 text-sm text-gray-600">
              {[batch?.mangoVariety, formatWeight(batch?.harvestQuantityKg), batch?.farmId, batch?.blockId]
                .filter(Boolean)
                .join(' · ')}
            </p>
          </div>
          <div className="text-right">
            <span
              className={`px-3 py-1 inline-flex text-sm font-semibold rounded-full ${getStatusColor(
                currentStatus,
              )}`}
            >
              {currentStatus || 'UNKNOWN'}
            </span>
            <p className="mt-2 text-xs text-gray-500">
              {recordedSteps} of {steps.length} steps recorded
            </p>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2" aria-label="Status chain">
          {STATUS_CHAIN.map((status, index) => {
            const reached = currentIndex >= 0 && index <= currentIndex;
            const isCurrent = status === currentStatus;
            return (
              <span key={status} className="flex items-center gap-2">
                {index > 0 && <span className="text-gray-300">→</span>}
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    isCurrent
                      ? 'bg-primary text-white'
                      : reached
                        ? 'bg-primary/10 text-primary'
                        : 'bg-gray-100 text-gray-500'
                  }`}
                >
                  {formatEnumLabel(status)}
                </span>
              </span>
            );
          })}
        </div>
      </header>

      <ol className="relative mt-6 space-y-6 border-l-2 border-gray-100 pl-6">
        {steps.map((step) => {
          const Icon = step.icon;
          return (
            <li key={step.key} className="relative">
              <span
                className={`absolute -left-[33px] top-0.5 flex h-4 w-4 items-center justify-center rounded-full border-2 ${
                  step.recorded ? 'border-primary bg-primary' : 'border-gray-300 bg-white'
                }`}
                aria-hidden="true"
              />
              <section className={CARD}>
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <h2 className="flex items-center gap-2 font-heading text-sm font-semibold text-gray-900">
                    <Icon
                      size={16}
                      aria-hidden="true"
                      className={step.recorded ? 'text-primary' : 'text-gray-400'}
                    />
                    {step.title}
                  </h2>
                  <span className={`text-xs ${step.recorded ? 'text-gray-600' : 'text-gray-400'}`}>
                    {step.recorded ? step.date || 'Date not set' : 'Not recorded yet'}
                  </span>
                </div>

                {step.recorded ? (
                  <StepDetails details={step.details} />
                ) : (
                  <p className="mt-2 text-sm text-gray-500">No record for this batch yet.</p>
                )}
              </section>
            </li>
          );
        })}
      </ol>
    </div>
  );
};

export default BatchDetail;
