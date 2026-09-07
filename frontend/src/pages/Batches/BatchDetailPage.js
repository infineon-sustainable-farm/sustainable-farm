import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { batchApi } from '../../services/api';
import { rawIntakeApi } from '../../services/api';
import { washingSortingApi } from '../../services/api';
import { dryingApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import WorkflowProgressionBar from '../../components/common/WorkflowProgressionBar';
import './BatchDetailPage.css';

const BatchDetailPage = () => {
  const { batchId } = useParams();
  const navigate = useNavigate();
  
  const [batch, setBatch] = React.useState(null);
  const [rawIntake, setRawIntake] = React.useState(null);
  const [washSortRecords, setWashSortRecords] = React.useState([]);
  const [dryingRuns, setDryingRuns] = React.useState([]);
  
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);
  const [advancing, setAdvancing] = React.useState(false);

  React.useEffect(() => {
    fetchBatchData();
  }, [batchId]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchBatchData = async () => {
    try {
      setLoading(true);
      
      // Fetch batch details
      const batchResponse = await batchApi.getById(batchId);
      setBatch(batchResponse.data);
      
      // Fetch related records by batchId
      try {
        const intakeResponse = await rawIntakeApi.getByBatchId(batchId);
        setRawIntake(intakeResponse.data);
      } catch (err) {
        console.log('No raw intake found for batch:', batchId);
        setRawIntake(null);
      }
      
      try {
        const washResponse = await washingSortingApi.getByBatchId(batchId);
        setWashSortRecords(washResponse.data || []);
      } catch (err) {
        console.log('No wash-sort records found for batch:', batchId);
        setWashSortRecords([]);
      }
      
      try {
        const dryingResponse = await dryingApi.getByBatchId(batchId);
        setDryingRuns(dryingResponse.data || []);
      } catch (err) {
        console.log('No drying runs found for batch:', batchId);
        setDryingRuns([]);
      }
      
      setError(null);
    } catch (err) {
      console.error('Error fetching batch data:', err);
      setError('Failed to load batch details.');
    } finally {
      setLoading(false);
    }
  };

  const handleAdvanceStatus = async () => {
    if (!batch) return;
    
    setAdvancing(true);
    try {
      const response = await batchApi.advanceStatus(batchId, {});
      setBatch(response.data);
      // Refresh related data after status change
      await fetchBatchData();
    } catch (err) {
      console.error('Error advancing batch status:', err);
      setError('Failed to advance batch status.');
    } finally {
      setAdvancing(false);
    }
  };

  const getStatusVariant = (status) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'SHIPPED': return 'success';
      case 'REJECTED': return 'error';
      case 'CREATED': return 'info';
      default: return 'warning';
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchBatchData} />;
  if (!batch) return <ErrorMessage message="Batch not found" />;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="header-left">
          <Button variant="secondary" onClick={() => navigate('/batches')}>
            <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>arrow_back</span>
            Back to Batches
          </Button>
          <h1>Batch Details: {batch.batchId}</h1>
        </div>
        <Badge variant={getStatusVariant(batch.currentStatus)}>{batch.currentStatus}</Badge>
      </div>

      {/* Workflow Progression */}
      <WorkflowProgressionBar 
        currentStatus={batch.currentStatus}
        onAdvance={handleAdvanceStatus}
        canAdvance={!advancing}
      />

      {/* Batch Information */}
      <Card className="detail-card">
        <h2>Batch Information</h2>
        <div className="detail-grid">
          <div className="detail-item">
            <label>Batch ID</label>
            <span>{batch.batchId}</span>
          </div>
          <div className="detail-item">
            <label>Harvest Date</label>
            <span>{batch.harvestDate}</span>
          </div>
          <div className="detail-item">
            <label>Mango Variety</label>
            <span>{batch.mangoVariety}</span>
          </div>
          <div className="detail-item">
            <label>Harvest Quantity</label>
            <span>{batch.harvestQuantityKg} kg</span>
          </div>
          <div className="detail-item">
            <label>Farm ID</label>
            <span>{batch.farmId}</span>
          </div>
          <div className="detail-item">
            <label>Block ID</label>
            <span>{batch.blockId}</span>
          </div>
          <div className="detail-item">
            <label>Current Status</label>
            <Badge variant={getStatusVariant(batch.currentStatus)}>{batch.currentStatus}</Badge>
          </div>
          <div className="detail-item">
            <label>Created At</label>
            <span>{new Date(batch.createdAt).toLocaleString()}</span>
          </div>
        </div>
      </Card>

      {/* Related Records */}
      <div className="related-records">
        {/* Raw Intake */}
        <Card className="related-card">
          <h3>Raw Intake</h3>
          {rawIntake ? (
            <div className="record-detail">
              <div className="detail-item">
                <label>Intake ID</label>
                <span>{rawIntake.intakeId}</span>
              </div>
              <div className="detail-item">
                <label>Source Farm</label>
                <span>{rawIntake.sourceFarm}</span>
              </div>
              <div className="detail-item">
                <label>Source Block</label>
                <span>{rawIntake.sourceBlock}</span>
              </div>
              <div className="detail-item">
                <label>Intake Date</label>
                <span>{rawIntake.intakeDate}</span>
              </div>
              <div className="detail-item">
                <label>Received Quantity</label>
                <span>{rawIntake.receivedQuantityKg} kg</span>
              </div>
              <div className="detail-item">
                <label>Received Variety</label>
                <span>{rawIntake.receivedVariety}</span>
              </div>
              <div className="detail-item">
                <label>Received Grade</label>
                <span>{rawIntake.receivedGrade}</span>
              </div>
              <div className="detail-item">
                <label>Intake Operator</label>
                <span>{rawIntake.intakeOperator}</span>
              </div>
            </div>
          ) : (
            <p className="no-records">No raw intake record found for this batch.</p>
          )}
        </Card>

        {/* Wash & Sort Records */}
        <Card className="related-card">
          <h3>Washing & Sorting Records</h3>
          {washSortRecords.length > 0 ? (
            <div className="records-list">
              {washSortRecords.map((record) => (
                <div key={record.recordId} className="record-item">
                  <div className="record-header">
                    <strong>{record.recordId}</strong>
                    <span>{new Date(record.startTime).toLocaleString()}</span>
                  </div>
                  <div className="record-details">
                    <div className="detail-item">
                      <label>Input Quantity</label>
                      <span>{record.inputQuantityKg} kg</span>
                    </div>
                    <div className="detail-item">
                      <label>Output Quantity</label>
                      <span>{record.outputQuantityKg} kg</span>
                    </div>
                    <div className="detail-item">
                      <label>Waste Quantity</label>
                      <span>{record.wasteQuantityKg} kg</span>
                    </div>
                    <div className="detail-item">
                      <label>Water Usage</label>
                      <span>{record.waterUsageLiters} L</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="no-records">No washing & sorting records found for this batch.</p>
          )}
        </Card>

        {/* Drying Runs */}
        <Card className="related-card">
          <h3>Drying Runs</h3>
          {dryingRuns.length > 0 ? (
            <div className="records-list">
              {dryingRuns.map((run) => (
                <div key={run.runId} className="record-item">
                  <div className="record-header">
                    <strong>{run.runId}</strong>
                    <span>{new Date(run.startTime).toLocaleString()}</span>
                  </div>
                  <div className="record-details">
                    <div className="detail-item">
                      <label>Duration</label>
                      <span>{run.durationHours} hours</span>
                    </div>
                    <div className="detail-item">
                      <label>Target Temperature</label>
                      <span>{run.targetTemperatureC}°C</span>
                    </div>
                    <div className="detail-item">
                      <label>Actual Temperature</label>
                      <span>{run.actualTemperatureC}°C</span>
                    </div>
                    <div className="detail-item">
                      <label>Start Moisture</label>
                      <span>{run.startMoisturePct}%</span>
                    </div>
                    <div className="detail-item">
                      <label>End Moisture</label>
                      <span>{run.endMoisturePct}%</span>
                    </div>
                    <div className="detail-item">
                      <label>Energy Usage</label>
                      <span>{run.energyUsageKwh} kWh</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="no-records">No drying runs found for this batch.</p>
          )}
        </Card>
      </div>
    </div>
  );
};

export default BatchDetailPage;