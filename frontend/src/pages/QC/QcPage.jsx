import React from 'react';
import { qcApi } from '../../services/api';
import Card from '../../shared/components/common/Card';
import Badge from '../../shared/components/common/Badge';
import Loading from '../../shared/components/common/Loading';
import ErrorMessage from '../../shared/components/common/ErrorMessage';
import Button from '../../shared/components/common/Button';
import ConfirmDialog from '../../shared/components/common/ConfirmDialog';
import SearchBar from '../../shared/components/common/SearchBar';
import FilterPanel from '../../shared/components/common/FilterPanel';
import './QcPage.css';

const QcPage = () => {
  const [qcCheckpoints, setQcCheckpoints] = React.useState([]);
  const [filteredCheckpoints, setFilteredCheckpoints] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  const [searchTerm, setSearchTerm] = React.useState('');
  const [activeFilters, setActiveFilters] = React.useState({});

  React.useEffect(() => {
    fetchQcCheckpoints();
  }, []);

  const fetchQcCheckpoints = async () => {
    try {
      setLoading(true);
      const response = await qcApi.getAll();
      setQcCheckpoints(response.data || []);
      setFilteredCheckpoints(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching QC checkpoints:', err);
      setError('Failed to load QC checkpoints.');
    } finally {
      setLoading(false);
    }
  };

  const filterDefinitions = [
    {
      name: 'stage',
      label: 'Processing Stage',
      type: 'select',
      options: [
        { value: 'WASHING', label: 'Washing' },
        { value: 'DRYING', label: 'Drying' },
        { value: 'PACKAGING', label: 'Packaging' },
        { value: 'COOLING', label: 'Cooling' },
      ],
      placeholder: 'All stages'
    },
    {
      name: 'result',
      label: 'QC Result',
      type: 'select',
      options: [
        { value: 'PASS', label: 'Pass' },
        { value: 'FAIL', label: 'Fail' },
        { value: 'REWORK', label: 'Rework' },
        { value: 'PENDING', label: 'Pending' },
      ],
      placeholder: 'All results'
    }
  ];

  React.useEffect(() => {
    let filtered = [...qcCheckpoints];

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(qc =>
        qc.checkpointId?.toLowerCase().includes(term) ||
        qc.batchId?.toLowerCase().includes(term) ||
        qc.stage?.toLowerCase().includes(term)
      );
    }

    if (activeFilters.stage) {
      filtered = filtered.filter(qc => qc.stage === activeFilters.stage);
    }
    if (activeFilters.result) {
      filtered = filtered.filter(qc => qc.result === activeFilters.result);
    }

    setFilteredCheckpoints(filtered);
  }, [searchTerm, activeFilters, qcCheckpoints]);

  const handleSearch = (term) => {
    setSearchTerm(term);
  };

  const handleFilterChange = (filters) => {
    setActiveFilters(filters);
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
      await qcApi.delete(record.checkpointId);
      setDeleteConfirm({ open: false, record: null });
      await fetchQcCheckpoints();
    } catch (err) {
      console.error('Error deleting QC checkpoint:', err);
      setError(`Failed to delete QC checkpoint ${record.checkpointId}.`);
    } finally {
      setDeleting(false);
    }
  };

  const getResultVariant = (result) => {
    switch (result) {
      case 'PASS': return 'success';
      case 'FAIL': return 'error';
      case 'REWORK': return 'warning';
      default: return 'info';
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchQcCheckpoints} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Quality Control Checkpoints</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New QC Checkpoint
        </Button>
      </div>

      <div className="search-filter-section">
        <SearchBar
          onSearch={handleSearch}
          placeholder="Search by checkpoint ID, batch ID, or stage..."
          initialValue={searchTerm}
        />
        <FilterPanel
          filters={filterDefinitions}
          onFilterChange={handleFilterChange}
          initialFilters={activeFilters}
        />
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Checkpoint ID</th>
                <th>Batch ID</th>
                <th>Stage</th>
                <th>Inspector</th>
                <th>Defects Count</th>
                <th>Result</th>
                <th>Checked At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredCheckpoints.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">
                  {qcCheckpoints.length === 0 ? 'No QC checkpoints found' : 'No QC checkpoints match your search or filters'}
                </td></tr>
              ) : (
                filteredCheckpoints.map((qc) => (
                  <tr key={qc.checkpointId}>
                    <td>{qc.checkpointId}</td>
                    <td>{qc.batchId}</td>
                    <td>{qc.stage}</td>
                    <td>{qc.inspectorName || 'N/A'}</td>
                    <td>{qc.defectsCount || 0}</td>
                    <td><Badge variant={getResultVariant(qc.result)}>{qc.result}</Badge></td>
                    <td>{new Date(qc.checkedAt).toLocaleString()}</td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(qc)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(qc)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete QC Checkpoint"
        message={`Are you sure you want to delete QC checkpoint "${deleteConfirm.record?.checkpointId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default QcPage;
