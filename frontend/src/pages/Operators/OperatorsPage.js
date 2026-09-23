import React from 'react';
import { operatorApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import OperatorForm from './OperatorForm';
import './OperatorsPage.css';

const OperatorsPage = () => {
  const [operators, setOperators] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchOperators();
  }, []);

  const fetchOperators = async () => {
    try {
      setLoading(true);
      const response = await operatorApi.getAll();
      setOperators(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching operators:', err);
      setError('Failed to load operators.');
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
      await operatorApi.delete(record.operatorId);
      setDeleteConfirm({ open: false, record: null });
      await fetchOperators();
    } catch (err) {
      console.error('Error deleting operator:', err);
      setError(`Failed to delete operator ${record.operatorId}.`);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchOperators} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Operators</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Operator
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Operator ID</th>
                <th>Name</th>
                <th>Role</th>
                <th>Status</th>
                <th>Hire Date</th>
                <th>Certifications</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {operators.length === 0 ? (
                <tr><td colSpan="7" className="empty-row">No operators found</td></tr>
              ) : (
                operators.map((op) => (
                  <tr key={op.operatorId}>
                    <td>{op.operatorId}</td>
                    <td>{op.operatorName}</td>
                    <td>{op.role}</td>
                    <td><Badge variant={op.activeStatus === 'ACTIVE' ? 'success' : 'neutral'}>{op.activeStatus}</Badge></td>
                    <td>{op.hireDate}</td>
                    <td>{op.certifications}</td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(op)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(op)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <OperatorForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={fetchOperators}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Operator"
        message={`Are you sure you want to delete operator "${deleteConfirm.record?.operatorName}" (${deleteConfirm.record?.operatorId})? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default OperatorsPage;