import { useState, useEffect } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { apiGetSamples } from '../services/api';
import Sidebar from '../components/layout/Sidebar';
import Topbar from '../components/layout/Topbar';
import DashboardPanel from '../components/panels/DashboardPanel';
import UploadPanel from '../components/panels/UploadPanel';
import ColonyCanvasPanel from '../components/panels/ColonyCanvasPanel';
import CFUPanel from '../components/panels/CFUPanel';
import ZonesPanel from '../components/panels/ZonesPanel';
import HistoryPanel from '../components/panels/HistoryPanel';
import SettingsPanel from '../components/panels/SettingsPanel';
import AdminPanel from '../components/panels/AdminPanel';
import { Navigate } from 'react-router-dom'; // eslint-disable-line

export default function AppDashboard() {
  const [samples, setSamples] = useState([]);
  const [activeSample, setActiveSample] = useState(null);
  const [toasts, setToasts] = useState([]);

  const showNotification = (type, message) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, type, message }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
  };

  const reloadSamples = async () => {
    try {
      const data = await apiGetSamples();
      setSamples(data.samples);
    } catch (err) {
      showNotification('danger', 'Failed to retrieve sample database log.');
    }
  };

  useEffect(() => { reloadSamples(); }, []);

  const sharedProps = { samples, activeSample, setActiveSample, showNotification, reloadSamples };

  return (
    <div className="app-layout">
      <Sidebar />
      <main className="app-content">
        <Topbar />
        <section className="workspace-panels">
          <Routes>
            <Route path="dashboard" element={<DashboardPanel {...sharedProps} />} />
            <Route path="upload" element={<UploadPanel {...sharedProps} />} />
            <Route path="analysis" element={<ColonyCanvasPanel {...sharedProps} />} />
            <Route path="cfu" element={<CFUPanel {...sharedProps} />} />
            <Route path="zones" element={<ZonesPanel {...sharedProps} />} />
            <Route path="history" element={<HistoryPanel {...sharedProps} />} />
            <Route path="settings" element={<SettingsPanel {...sharedProps} />} />
            <Route path="admin" element={<AdminPanel {...sharedProps} />} />
            <Route path="*" element={<Navigate to="dashboard" replace />} />
          </Routes>
        </section>
      </main>

      {/* Toast Notifications */}
      <div style={{ position: 'fixed', bottom: 20, right: 20, zIndex: 1000, display: 'flex', flexDirection: 'column', gap: 10, width: 340, maxWidth: '90%' }}>
        {toasts.map(t => (
          <div key={t.id} className={`glass-card badge-${t.type}`} style={{ padding: '14px 18px', borderRadius: 10, fontWeight: 600, fontSize: 13, boxShadow: '0 8px 30px rgba(0,0,0,0.15)', animation: 'slideInRight 0.3s ease', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', borderLeft: `4px solid ${t.type === 'danger' ? 'var(--color-danger)' : 'var(--color-success)'}` }}>
            {t.message}
          </div>
        ))}
      </div>
    </div>
  );
}
