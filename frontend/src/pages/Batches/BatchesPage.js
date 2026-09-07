import React from 'react';
import { useNavigate } from 'react-router-dom';
import { batchApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import BatchForm from './BatchForm';
import './BatchesPage.css';

const BatchesPage = () => {
  const navigate = useNavigate();
  const [batches, setBatches] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchBatches();
  }, []);

  const fetchBatches = async () => {
    try {
      setLoading(true);
      const response = await batchApi.getAll();
      setBatches(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching batches:', err);
      setError('Failed to load batches.');
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
      await batchApi.delete(record.batchId);
      setDeleteConfirm({ open: false, record: null });
      await fetchBatches();
    } catch (err) {
      console.error('Error deleting batch:', err);
      setError(`Failed to delete batch ${record.batchId}.`);
    } finally {
      setDeleting(false);
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
  if (error) return <ErrorMessage message={error} onRetry={fetchBatches} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Batches</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Batch
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Batch ID</th>
                <th>Harvest Date</th>
                <th>Variety</th>
                <th>Farm/Block</th>
                <th>Quantity (kg)</th>
                <th>Status</th>
                <th>Workflow</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {batches.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">No batches found</td></tr>
              ) : (
                batches.map((batch) => (
                  <tr key={batch.batchId}>
                    <td>{batch.batchId}</td>
                    <td>{batch.harvestDate}</td>
                    <td>{batch.mangoVariety}</td>
                    <td>{batch.farmId} / {batch.blockId}</td>
                    <td>{batch.harvestQuantityKg}</td>
                    <td><Badge variant={getStatusVariant(batch.currentStatus)}>{batch.currentStatus}</Badge></td>
                    <td>
                      <MiniWorkflowIndicator currentStatus={batch.currentStatus} />
                    </td>
                    <td>
                      <div className="row-actions">
                        <Button variant="info" size="sm" onClick={() => navigate(`/batches/${batch.batchId}`)}>View</Button>
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(batch)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(batch)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <BatchForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={fetchBatches}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Batch"
        message={`Are you sure you want to delete batch "${deleteConfirm.record?.batchId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

// Mini workflow indicator for table rows
function MiniWorkflowIndicator({ currentStatus }) {
  const stages = ['CREATED', 'INTAKE', 'WASHING', 'DRYING', 'PACKAGING', 'COMPLETED', 'SHIPPED'];
  const currentIndex = stages.indexOf(currentStatus);
  
  if (currentIndex === -1) {
    return <span className="mini-workflow unknown">?</span>;
  }

  return (
    <div className="mini-workflow">
      {stages.map((stage, index) => {
        let dotClass = 'workflow-dot';
        if (index < currentIndex) dotClass += ' completed';
        else if (index === currentIndex) dotClass += ' current';
        else dotClass += ' pending';
        
        return <div key={stage} className={dotClass} title={stage} />;
      })}
    </div>
  );
}

export default BatchesPage;