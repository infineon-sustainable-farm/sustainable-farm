import React from 'react';
import { dryingApi } from '../../services/api';
import Card from '../../components/common/Card';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import DryingForm from './DryingForm';
import './DryingPage.css';

const DryingPage = () => {
  const [dryingRuns, setDryingRuns] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchDryingRuns();
  }, []);

  const fetchDryingRuns = async () => {
    try {
      setLoading(true);
      const response = await dryingApi.getAll();
      setDryingRuns(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching drying runs:', err);
      setError('Failed to load drying runs.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setFormOpen(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    setFormOpen(true);
  };

  const handleDeleteRequest = (record) => {
    setDeleteConfirm({ open: true, record });
  };

  const handleDeleteConfirm = async () => {
    const record = deleteConfirm.record;
    if (!record) return;
    setDeleting(true);
    try {
      await dryingApi.delete(record.runId);
      setDeleteConfirm({ open: false, record: null });
      await fetchDryingRuns();
    } catch (err) {
      console.error('Error deleting run:', err);
      setError(`Failed to delete drying run ${record.runId}.`);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchDryingRuns} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Drying Runs</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Run
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Run ID</th>
                <th>Batch</th>
                <th>Start Time</th>
                <th>Duration (h)</th>
                <th>Temp (°C)</th>
                <th>Start Moisture (%)</th>
                <th>End Moisture (%)</th>
                <th>Energy (kWh)</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {dryingRuns.length === 0 ? (
                <tr><td colSpan="9" className="empty-row">No drying runs found</td></tr>
              ) : (
                dryingRuns.map((run) => (
                  <tr key={run.runId}>
                    <td>{run.runId}</td>
                    <td>{run.batchId}</td>
                    <td>{run.startTime}</td>
                    <td>{run.durationHours}</td>
                    <td>{run.actualTemperatureC}</td>
                    <td>{run.startMoisturePct}</td>
                    <td>{run.endMoisturePct}</td>
                    <td>{run.energyUsageKwh}</td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(run)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(run)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <DryingForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={fetchDryingRuns}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Drying Run"
        message={`Are you sure you want to delete drying run "${deleteConfirm.record?.runId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default DryingPage;