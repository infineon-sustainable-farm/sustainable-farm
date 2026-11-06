import React from 'react';
import Card from '../../shared/components/common/Card';
import Badge from '../../shared/components/common/Badge';
import Loading from '../../shared/components/common/Loading';
import ErrorMessage from '../../shared/components/common/ErrorMessage';
import Button from '../../shared/components/common/Button';
import SearchBar from '../../shared/components/common/SearchBar';
import FilterPanel from '../../shared/components/common/FilterPanel';
import './CompliancePage.css';

const CompliancePage = () => {
  const [complianceRecords, setComplianceRecords] = React.useState([]);
  const [filteredRecords, setFilteredRecords] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [searchTerm, setSearchTerm] = React.useState('');
  const [activeFilters, setActiveFilters] = React.useState({});

  React.useEffect(() => {
    fetchComplianceRecords();
  }, []);

  const fetchComplianceRecords = async () => {
    try {
      setLoading(true);
      // Mock data for now - replace with actual API call
      const mockData = [
        {
          recordId: 'COMP-001',
          batchId: 'BATCH-001',
          complianceType: 'HACCP',
          result: 'COMPLIANT',
          auditor: 'John Doe',
          auditDate: '2026-10-15',
          notes: 'All checks passed'
        },
        {
          recordId: 'COMP-002',
          batchId: 'BATCH-002',
          complianceType: 'FOOD_SAFETY',
          result: 'PENDING',
          auditor: 'Jane Smith',
          auditDate: '2026-10-16',
          notes: 'Awaiting final inspection'
        }
      ];
      setComplianceRecords(mockData);
      setFilteredRecords(mockData);
      setError(null);
    } catch (err) {
      console.error('Error fetching compliance records:', err);
      setError('Failed to load compliance records.');
    } finally {
      setLoading(false);
    }
  };

  const filterDefinitions = [
    {
      name: 'type',
      label: 'Compliance Type',
      type: 'select',
      options: [
        { value: 'HACCP', label: 'HACCP' },
        { value: 'FOOD_SAFETY', label: 'Food Safety' },
        { value: 'EU_EXPORT', label: 'EU Export' },
        { value: 'HYGIENE', label: 'Hygiene' },
        { value: 'TRACEABILITY', label: 'Traceability' },
      ],
      placeholder: 'All types'
    },
    {
      name: 'result',
      label: 'Compliance Result',
      type: 'select',
      options: [
        { value: 'COMPLIANT', label: 'Compliant' },
        { value: 'NON_COMPLIANT', label: 'Non-Compliant' },
        { value: 'PENDING', label: 'Pending' },
      ],
      placeholder: 'All results'
    }
  ];

  React.useEffect(() => {
    let filtered = [...complianceRecords];

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(record =>
        record.recordId?.toLowerCase().includes(term) ||
        record.batchId?.toLowerCase().includes(term) ||
        record.complianceType?.toLowerCase().includes(term)
      );
    }

    if (activeFilters.type) {
      filtered = filtered.filter(record => record.complianceType === activeFilters.type);
    }
    if (activeFilters.result) {
      filtered = filtered.filter(record => record.result === activeFilters.result);
    }

    setFilteredRecords(filtered);
  }, [searchTerm, activeFilters, complianceRecords]);

  const handleSearch = (term) => {
    setSearchTerm(term);
  };

  const handleFilterChange = (filters) => {
    setActiveFilters(filters);
  };

  const getResultVariant = (result) => {
    switch (result) {
      case 'COMPLIANT': return 'success';
      case 'NON_COMPLIANT': return 'error';
      default: return 'warning';
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchComplianceRecords} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Compliance Records</h1>
        <Button variant="primary">
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Compliance Record
        </Button>
      </div>

      <div className="search-filter-section">
        <SearchBar
          onSearch={handleSearch}
          placeholder="Search by record ID, batch ID, or type..."
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
                <th>Record ID</th>
                <th>Batch ID</th>
                <th>Compliance Type</th>
                <th>Result</th>
                <th>Auditor</th>
                <th>Audit Date</th>
                <th>Notes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRecords.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">
                  {complianceRecords.length === 0 ? 'No compliance records found' : 'No compliance records match your search or filters'}
                </td></tr>
              ) : (
                filteredRecords.map((record) => (
                  <tr key={record.recordId}>
                    <td>{record.recordId}</td>
                    <td>{record.batchId}</td>
                    <td>{record.complianceType}</td>
                    <td><Badge variant={getResultVariant(record.result)}>{record.result}</Badge></td>
                    <td>{record.auditor}</td>
                    <td>{record.auditDate}</td>
                    <td>{record.notes}</td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm">View</Button>
                        <Button variant="secondary" size="sm">Edit</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

export default CompliancePage;
