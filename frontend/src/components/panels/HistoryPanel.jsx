import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiDeleteSample, apiExportCSV, apiGetPdfUrl } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

// BUG-014/026 FIX: Pagination constants — renders only PAGE_SIZE rows at a time
const PAGE_SIZE = 20;

// Helper: Firestore returns id (not _id). Support both defensively.
const getId = (s) => s.id || s._id;

export default function HistoryPanel({ samples, setActiveSample, showNotification, reloadSamples }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user && user.role === 'Admin';
  
  const [filters, setFilters] = useState({
    search: '',
    applianceType: 'All',
    riskLevel: 'All',
    department: 'All',
    role: 'All',
    sort: 'newest'
  });
  const [page, setPage] = useState(1);

  // Reset to page 1 when filters change
  useEffect(() => { setPage(1); }, [filters]);

  // Admin Metrics Calculation
  const metrics = useMemo(() => {
    if (!isAdmin || !samples) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todaysUploads = samples.filter(s => new Date(s.createdAt) >= today).length;
    const uniqueUsers = new Set(samples.map(s => s.userName || s.operatorName)).size;
    
    // Sort by recent for recent uploads count (just using length of something? "Recent Uploads" could be last 7 days)
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    const recentUploads = samples.filter(s => new Date(s.createdAt) >= sevenDaysAgo).length;

    return {
      total: samples.length,
      today: todaysUploads,
      users: uniqueUsers,
      recent: recentUploads
    };
  }, [samples, isAdmin]);

  // Memoized filter + sort — avoids re-computing on every keystroke unless deps change
  const filtered = useMemo(() => {
    let list = [...samples];
    if (filters.search) {
      const q = filters.search.toLowerCase();
      list = list.filter(s =>
        s.batchId?.toLowerCase().includes(q) ||
        s.operatorName?.toLowerCase().includes(q) ||
        s.userName?.toLowerCase().includes(q) ||
        s.email?.toLowerCase().includes(q)
      );
    }
    if (filters.applianceType !== 'All') list = list.filter(s => s.applianceType === filters.applianceType);
    if (filters.riskLevel !== 'All') list = list.filter(s => s.contaminationRisk === filters.riskLevel);
    if (filters.department !== 'All') list = list.filter(s => (s.department || 'General Microbiology') === filters.department);
    if (filters.role !== 'All') list = list.filter(s => (s.role || 'Lab Technician') === filters.role);
    
    if (filters.sort === 'oldest') list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    else if (filters.sort === 'count-high') list.sort((a, b) => b.colonyCount - a.colonyCount);
    else if (filters.sort === 'count-low') list.sort((a, b) => a.colonyCount - b.colonyCount);
    else list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return list;
  }, [samples, filters]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const handleDelete = async (s) => {
    const id = getId(s);
    if (!id) { showNotification('danger', 'Cannot delete: sample has no valid ID.'); return; }
    if (!window.confirm('Delete this sample record permanently? This action cannot be undone.')) return;
    try {
      await apiDeleteSample(id);
      showNotification('success', 'Sample record deleted successfully.');
      await reloadSamples();
    } catch (err) {
      showNotification('danger', err.message || 'Failed to delete sample.');
    }
  };

  const handleView = (sample) => {
    setActiveSample(sample);
    navigate('/app/analysis');
  };

  const handlePdf = async (sample) => {
    const id = getId(sample);
    if (!id) { showNotification('danger', 'Cannot generate PDF: sample has no valid ID.'); return; }
    const url = await apiGetPdfUrl(id);
    window.open(url, '_blank');
  };

  const handleCSVExport = async () => {
    try {
      await apiExportCSV();
    } catch (err) {
      showNotification('danger', err.message || 'Failed to export CSV.');
    }
  };

  const handleFilterChange = (field, value) => setFilters(prev => ({ ...prev, [field]: value }));

  return (
    <div id="history-panel" className="panel-view active">
      <div className="panel-header">
        <h2>Sanitization Verification History Logs</h2>
        <p>Query, search, sort, and export comprehensive quality control datasets. {filtered.length} record{filtered.length !== 1 ? 's' : ''} found.</p>
      </div>

      {isAdmin && metrics && (
        <div className="metrics-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '20px' }}>
          <div className="glass-card" style={{ padding: '20px', textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: '24px', color: 'var(--primary)' }}>{metrics.total}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 600 }}>Total Samples</p>
          </div>
          <div className="glass-card" style={{ padding: '20px', textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: '24px', color: 'var(--primary)' }}>{metrics.today}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 600 }}>Today's Uploads</p>
          </div>
          <div className="glass-card" style={{ padding: '20px', textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: '24px', color: 'var(--primary)' }}>{metrics.users}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 600 }}>Unique Users</p>
          </div>
          <div className="glass-card" style={{ padding: '20px', textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: '24px', color: 'var(--primary)' }}>{metrics.recent}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: 600 }}>Recent (7 Days)</p>
          </div>
        </div>
      )}

      {/* Filters Bar */}
      <div className="glass-card table-filters">
        <div className="table-filter-group">
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Search</label>
          <input
            type="text"
            className="form-input"
            style={{ padding: '8px 12px', marginTop: 6 }}
            placeholder="Batch / Operator / Email..."
            value={filters.search}
            onChange={e => handleFilterChange('search', e.target.value)}
          />
        </div>
        <div className="table-filter-group">
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Appliance</label>
          <select className="form-input" style={{ padding: '8px 12px', marginTop: 6 }} value={filters.applianceType} onChange={e => handleFilterChange('applianceType', e.target.value)}>
            {['All', 'Catheter', 'Surgical Syringe', 'Scalpel', 'Endoscope Tube', 'Petri Dish (Control)'].map(o => (
              <option key={o} value={o}>{o === 'All' ? 'All Appliances' : o}</option>
            ))}
          </select>
        </div>
        <div className="table-filter-group">
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Risk Level</label>
          <select className="form-input" style={{ padding: '8px 12px', marginTop: 6 }} value={filters.riskLevel} onChange={e => handleFilterChange('riskLevel', e.target.value)}>
            {['All', 'Low', 'Medium', 'High', 'Critical'].map(o => (
              <option key={o} value={o}>{o === 'All' ? 'All Risks' : o}</option>
            ))}
          </select>
        </div>
        {isAdmin && (
          <>
            <div className="table-filter-group">
              <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Department</label>
              <select className="form-input" style={{ padding: '8px 12px', marginTop: 6 }} value={filters.department} onChange={e => handleFilterChange('department', e.target.value)}>
                {['All', 'General Microbiology', 'Infectious Diseases', 'Sterilization Unit', 'Quality Assurance'].map(o => (
                  <option key={o} value={o}>{o === 'All' ? 'All Depts' : o}</option>
                ))}
              </select>
            </div>
            <div className="table-filter-group">
              <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Role</label>
              <select className="form-input" style={{ padding: '8px 12px', marginTop: 6 }} value={filters.role} onChange={e => handleFilterChange('role', e.target.value)}>
                {['All', 'Researcher', 'Lab Technician', 'Admin'].map(o => (
                  <option key={o} value={o}>{o === 'All' ? 'All Roles' : o}</option>
                ))}
              </select>
            </div>
          </>
        )}
        <div className="table-filter-group">
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Chronology</label>
          <select className="form-input" style={{ padding: '8px 12px', marginTop: 6 }} value={filters.sort} onChange={e => handleFilterChange('sort', e.target.value)}>
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="count-high">Colony Count (High › Low)</option>
            <option value="count-low">Colony Count (Low › High)</option>
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={handleCSVExport} style={{ padding: '10px 16px', fontSize: 12, display: 'inline-flex', gap: 6 }}>
            <svg width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Export CSV Table
          </button>
        </div>
      </div>

      {/* Data Table */}
      <div className="glass-card">
        <div className="history-table-wrapper">
          <table className="history-table">
            <thead>
              <tr>
                {isAdmin && <th>Uploaded By</th>}
                {isAdmin && <th>Role / Dept</th>}
                <th>Batch Identifier</th>
                <th>Appliance Material</th>
                <th style={{ textAlign: 'center' }}>Colony Load</th>
                <th style={{ textAlign: 'center' }}>CFU Value</th>
                <th>Alert Status</th>
                <th>Date Logged</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 9 : 7} style={{ padding: 30, textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No matching records found. Try adjusting your filters.
                  </td>
                </tr>
              ) : paginated.map(s => (
                <tr key={getId(s)}>
                  {isAdmin && (
                    <td style={{ padding: 12 }}>
                      <div style={{ fontWeight: 600 }}>{s.userName || s.operatorName}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{s.email || 'N/A'}</div>
                    </td>
                  )}
                  {isAdmin && (
                    <td style={{ padding: 12 }}>
                      <div style={{ fontWeight: 500 }}>{s.role || 'Lab Technician'}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{s.department || 'General Microbiology'}</div>
                    </td>
                  )}
                  <td style={{ padding: 12, fontWeight: 700 }}>{s.batchId}</td>
                  <td style={{ padding: 12, color: 'var(--text-secondary)' }}>{s.applianceType}</td>
                  <td style={{ padding: 12, textAlign: 'center' }}>{s.colonyCount}</td>
                  <td style={{ padding: 12, textAlign: 'center' }}>{s.cfuCount}</td>
                  <td style={{ padding: 12 }}>
                    <span className={`badge badge-${(s.contaminationRisk || 'low').toLowerCase()}`}>
                      {s.contaminationRisk}
                    </span>
                  </td>
                  <td style={{ padding: 12 }}>{new Date(s.createdAt).toLocaleDateString()}</td>

                  <td style={{ padding: 12 }}>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        id={`btn-view-${getId(s)}`}
                        className="btn btn-secondary"
                        style={{ padding: '5px 10px', fontSize: 11 }}
                        onClick={() => handleView(s)}
                        title="View colony analysis"
                      >
                        View
                      </button>
                      <button
                        id={`btn-pdf-${getId(s)}`}
                        className="btn btn-secondary"
                        style={{ padding: '5px 10px', fontSize: 11 }}
                        onClick={() => handlePdf(s)}
                        title="Download PDF certificate"
                      >
                        PDF
                      </button>
                      <button
                        id={`btn-delete-${getId(s)}`}
                        className="btn btn-secondary"
                        style={{ padding: '5px 10px', fontSize: 11, color: 'var(--color-danger)' }}
                        onClick={() => handleDelete(s)}
                        title="Permanently delete record"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* BUG-014/026 FIX: Pagination controls */}
        {totalPages > 1 && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderTop: '1px solid var(--border-color)',
            fontSize: 13
          }}>
            <span style={{ color: 'var(--text-secondary)' }}>
              Showing {Math.min((page - 1) * PAGE_SIZE + 1, filtered.length)}–{Math.min(page * PAGE_SIZE, filtered.length)} of {filtered.length} records
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                className="btn btn-secondary"
                style={{ padding: '5px 12px', fontSize: 12 }}
                disabled={page === 1}
                onClick={() => setPage(p => p - 1)}
              >
                ← Previous
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                const pageNum = Math.max(1, Math.min(page - 2, totalPages - 4)) + i;
                return (
                  <button
                    key={pageNum}
                    className="btn btn-secondary"
                    style={{
                      padding: '5px 10px',
                      fontSize: 12,
                      background: pageNum === page ? 'var(--primary)' : '',
                      color: pageNum === page ? 'white' : ''
                    }}
                    onClick={() => setPage(pageNum)}
                  >
                    {pageNum}
                  </button>
                );
              })}
              <button
                className="btn btn-secondary"
                style={{ padding: '5px 12px', fontSize: 12 }}
                disabled={page === totalPages}
                onClick={() => setPage(p => p + 1)}
              >
                Next →
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
