import React from 'react';
import { washingSortingApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import WashingSortingForm from './WashingSortingForm';
import './WashingSortingPage.css';

const WashingSortingPage = () => {
  const [records, setRecords] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchRecords();
  }, []);

  const fetchRecords = async () => {
    try {
      setLoading(true);
      const response = await washingSortingApi.getAll();
      setRecords(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching wash sort records:', err);
      setError('Failed to load washing & sorting records.');
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
      await washingSortingApi.delete(record.recordId);
      setDeleteConfirm({ open: false, record: null });
      await fetchRecords();
    } catch (err) {
      console.error('Error deleting record:', err);
      setError(`Failed to delete record ${record.recordId}.`);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchRecords} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Washing & Sorting</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Record
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Record ID</th>
                <th>Batch</th>
                <th>Start Time</th>
                <th>Input (kg)</th>
                <th>Output (kg)</th>
                <th>Waste (kg)</th>
                <th>Water (L)</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {records.length === 0 ? (
                <tr><td colSpan="9" className="empty-row">No washing & sorting records found</td></tr>
              ) : (
                records.map((record) => (
                  <tr key={record.recordId}>
                    <td>{record.recordId}</td>
                    <td>{record.batchId}</td>
                    <td>{record.startTime}</td>
                    <td>{record.inputQuantityKg}</td>
                    <td>{record.outputQuantityKg}</td>
                    <td>{record.wasteQuantityKg}</td>
                    <td>{record.waterUsageLiters}</td>
                    <td><Badge variant={record.endTime ? 'success' : 'warning'}>{record.endTime ? 'Completed' : 'In Progress'}</Badge></td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(record)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(record)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <WashingSortingForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={fetchRecords}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Wash/Sort Record"
        message={`Are you sure you want to delete record "${deleteConfirm.record?.recordId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default WashingSortingPage;