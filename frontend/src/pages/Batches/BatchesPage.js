import React from 'react';
import { useNavigate } from 'react-router-dom';
import { batchApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import SearchBar from '../../components/common/SearchBar';
import FilterPanel from '../../components/common/FilterPanel';
import BatchForm from './BatchForm';
import './BatchesPage.css';

const BatchesPage = () => {
  const navigate = useNavigate();
  const [batches, setBatches] = React.useState([]);
  const [filteredBatches, setFilteredBatches] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  const [searchTerm, setSearchTerm] = React.useState('');
  const [activeFilters, setActiveFilters] = React.useState({});

  React.useEffect(() => {
    fetchBatches();
  }, []);

  const fetchBatches = async () => {
    try {
      setLoading(true);
      const response = await batchApi.getAll();
      setBatches(response.data || []);
      setFilteredBatches(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching batches:', err);
      setError('Failed to load batches.');
    } finally {
      setLoading(false);
    }
  };

  // Filter definitions
  const filterDefinitions = [
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { value: 'CREATED', label: 'Created' },
        { value: 'INTAKE', label: 'Intake' },
        { value: 'WASHING', label: 'Washing' },
        { value: 'DRYING', label: 'Drying' },
        { value: 'PACKAGING', label: 'Packaging' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'SHIPPED', label: 'Shipped' },
        { value: 'REJECTED', label: 'Rejected' },
      ],
      placeholder: 'All statuses'
    },
    {
      name: 'variety',
      label: 'Mango Variety',
      type: 'select',
      options: [
        { value: 'KEITT', label: 'Keitt' },
        { value: 'KENT', label: 'Kent' },
        { value: 'TOMMY', label: 'Tommy' },
        { value: 'AMÉLIE', label: 'Amélie' },
        { value: 'OTHER', label: 'Other' },
      ],
      placeholder: 'All varieties'
    },
    {
      name: 'dateRange',
      label: 'Harvest Date Range',
      type: 'date-range'
    }
  ];

  // Apply search and filters
  React.useEffect(() => {
    let filtered = [...batches];

    // Apply search
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(batch =>
        batch.batchId?.toLowerCase().includes(term) ||
        batch.mangoVariety?.toLowerCase().includes(term) ||
        batch.farmId?.toLowerCase().includes(term) ||
        batch.currentStatus?.toLowerCase().includes(term)
      );
    }

    // Apply filters
    if (activeFilters.status) {
      filtered = filtered.filter(batch => batch.currentStatus === activeFilters.status);
    }
    if (activeFilters.variety) {
      filtered = filtered.filter(batch => batch.mangoVariety === activeFilters.variety);
    }
    if (activeFilters.dateRange_from || activeFilters.dateRange_to) {
      filtered = filtered.filter(batch => {
        const batchDate = new Date(batch.harvestDate);
        const fromDate = activeFilters.dateRange_from ? new Date(activeFilters.dateRange_from) : null;
        const toDate = activeFilters.dateRange_to ? new Date(activeFilters.dateRange_to) : null;
        
        if (fromDate && batchDate < fromDate) return false;
        if (toDate && batchDate > toDate) return false;
        return true;
      });
    }

    setFilteredBatches(filtered);
  }, [searchTerm, activeFilters, batches]);

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

      {/* Search and Filter Section */}
      <div className="search-filter-section">
        <SearchBar
          onSearch={handleSearch}
          placeholder="Search by batch ID, variety, farm, or status..."
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
              {filteredBatches.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">
                  {batches.length === 0 ? 'No batches found' : 'No batches match your search or filters'}
                </td></tr>
              ) : (
                filteredBatches.map((batch) => (
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