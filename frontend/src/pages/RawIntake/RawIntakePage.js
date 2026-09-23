import React from 'react';
import { rawIntakeApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import RawIntakeForm from './RawIntakeForm';
import './RawIntakePage.css';

const RawIntakePage = () => {
  const [intakes, setIntakes] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchIntakes();
  }, []);

  const fetchIntakes = async () => {
    try {
      setLoading(true);
      const response = await rawIntakeApi.getAll();
      setIntakes(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching raw intakes:', err);
      setError('Failed to load raw intake records.');
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
      await rawIntakeApi.delete(record.intakeId);
      setDeleteConfirm({ open: false, record: null });
      await fetchIntakes();
    } catch (err) {
      console.error('Error deleting intake:', err);
      setError(`Failed to delete intake ${record.intakeId}.`);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchIntakes} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Raw Intake</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Intake
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Intake ID</th>
                <th>Date</th>
                <th>Source Farm</th>
                <th>Variety</th>
                <th>Grade</th>
                <th>Received (kg)</th>
                <th>Operator</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {intakes.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">No raw intake records found</td></tr>
              ) : (
                intakes.map((intake) => (
                  <tr key={intake.intakeId}>
                    <td>{intake.intakeId}</td>
                    <td>{intake.intakeDate}</td>
                    <td>{intake.sourceFarm}</td>
                    <td>{intake.receivedVariety}</td>
                    <td><Badge variant="info">{intake.receivedGrade}</Badge></td>
                    <td>{intake.receivedQuantityKg}</td>
                    <td>{intake.intakeOperator}</td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(intake)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(intake)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <RawIntakeForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={fetchIntakes}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Raw Intake"
        message={`Are you sure you want to delete intake "${deleteConfirm.record?.intakeId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default RawIntakePage;