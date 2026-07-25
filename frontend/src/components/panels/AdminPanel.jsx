import React, { useState, useEffect } from 'react';
import { apiGetAdminUsers, apiUpdateUserRole, apiDeleteUser, apiGetAdminHistory, apiGetPdfUrl } from '../../services/api';

const ROLES = ['Lab Technician', 'Researcher', 'Admin'];

export default function AdminPanel({ showNotification }) {
  const [users, setUsers] = useState([]);
  const [adminSamples, setAdminSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [samplesLoading, setSamplesLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await apiGetAdminUsers();
      setUsers(data.users);
    } catch (err) {
      showNotification('danger', 'Failed to load user list: ' + err.message);
    } finally { setLoading(false); }
  };

  const loadAdminSamples = async () => {
    setSamplesLoading(true);
    try {
      const data = await apiGetAdminHistory();
      setAdminSamples(data.samples);
    } catch (err) {
      showNotification('danger', 'Failed to load admin history: ' + err.message);
    } finally { setSamplesLoading(false); }
  };


  useEffect(() => { 
    loadUsers(); 
    loadAdminSamples();
  }, []);

  const handleRoleChange = async (userId, role) => {
    try {
      await apiUpdateUserRole(userId, role);
      showNotification('success', `User role updated to ${role}.`);
      await loadUsers();
    } catch (err) {
      showNotification('danger', err.message);
    }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Permanently revoke this user account?')) return;
    try {
      await apiDeleteUser(userId);
      showNotification('success', 'User account revoked.');
      await loadUsers();
    } catch (err) {
      showNotification('danger', err.message);
    }
  };

  return (
    <div id="admin-panel" className="panel-view active">
      <div className="panel-header"><h2>Authorized Personnel Registry</h2><p>Revoke user login credentials, assign operator functional roles, and audit security access tokens.</p></div>
      {selectedUser && (
        <div className="modal-overlay" style={{ display: 'flex' }} onClick={() => setSelectedUser(null)}>
          <div className="modal-container glass-card" onClick={e => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={() => setSelectedUser(null)}>&times;</button>
            <h3 style={{ marginBottom: 20 }}>{selectedUser.name}</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, fontSize: 13 }}>
              {[['Email', selectedUser.email], ['Role', selectedUser.role], ['Department', selectedUser.department || 'N/A'], ['2FA', selectedUser.twoFactorEnabled ? 'Enabled' : 'Disabled']].map(([k, v]) => (
                <React.Fragment key={k}><div style={{ color: 'var(--text-secondary)' }}>{k}:</div><div><strong>{v}</strong></div></React.Fragment>
              ))}
            </div>
          </div>
        </div>
      )}
      <div className="glass-card">
        <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>System User Accounts</h3>
        {loading ? (
          <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 30 }}>Loading user registry...</p>
        ) : (
          <div className="history-table-wrapper">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Full Operator Name</th><th>Laboratory Email Address</th><th>Assigned Role Authorization</th><th>Date Created</th><th style={{ textAlign: 'right' }}>Access State</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.id || u._id}>
                    <td style={{ padding: 12, fontWeight: 700, cursor: 'pointer', color: 'var(--primary)' }} onClick={() => setSelectedUser(u)}>{u.name}</td>
                    <td style={{ padding: 12, color: 'var(--text-secondary)' }}>{u.email}</td>
                    <td style={{ padding: 12 }}>
                      <select className="form-input" style={{ padding: '4px 8px', fontSize: 12, width: 'auto' }} value={u.role} onChange={e => handleRoleChange(u.id || u._id, e.target.value)}>
                        {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                      </select>
                    </td>
                    <td style={{ padding: 12 }}>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'N/A'}</td>
                    <td style={{ padding: 12, textAlign: 'right' }}>
                      <button className="btn btn-secondary" style={{ padding: '5px 10px', fontSize: 11, color: 'var(--color-danger)' }} onClick={() => handleDelete(u.id || u._id)}>
                        Revoke Access
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="glass-card" style={{ marginTop: 20 }}>
        <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>Global Samples History (Admin Access)</h3>
        {samplesLoading ? (
          <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 30 }}>Loading global samples...</p>
        ) : (
          <div className="history-table-wrapper">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Uploaded By</th>
                  <th>Role / Dept</th>
                  <th>Batch ID</th>
                  <th>Appliance</th>
                  <th>CFU Value</th>
                  <th>Risk</th>
                  <th>Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {adminSamples.length === 0 ? (
                  <tr><td colSpan="8" style={{ textAlign: 'center', padding: 20 }}>No samples found in the system.</td></tr>
                ) : (
                  adminSamples.map(s => (
                    <tr key={s.id || s._id}>
                      <td style={{ padding: 12 }}>
                        <div style={{ fontWeight: 600 }}>{s.userName || s.operatorName}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{s.email || 'N/A'}</div>
                      </td>
                      <td style={{ padding: 12 }}>
                        <div style={{ fontWeight: 500 }}>{s.role || 'Lab Technician'}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{s.department || 'General Microbiology'}</div>
                      </td>
                      <td style={{ padding: 12, fontWeight: 700 }}>{s.batchId}</td>
                      <td style={{ padding: 12, color: 'var(--text-secondary)' }}>{s.applianceType}</td>
                      <td style={{ padding: 12, textAlign: 'center' }}>{s.cfuCount}</td>
                      <td style={{ padding: 12 }}>
                        <span className={`badge badge-${(s.contaminationRisk || 'low').toLowerCase()}`}>{s.contaminationRisk}</span>
                      </td>
                      <td style={{ padding: 12 }}>{new Date(s.createdAt).toLocaleDateString()}</td>
                      <td style={{ padding: 12 }}>
                        <button className="btn btn-secondary" style={{ padding: '5px 10px', fontSize: 11 }} onClick={async () => window.open(await apiGetPdfUrl(s.id || s._id), '_blank')}>PDF</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
