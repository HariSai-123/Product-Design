import { useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const BREADCRUMB_MAP = {
  '/app/dashboard': 'Dashboard',
  '/app/upload': 'Upload & Analyze',
  '/app/analysis': 'Colony Canvas',
  '/app/cfu': 'CFU & Density',
  '/app/zones': 'Zone Analysis',
  '/app/history': 'Sample History',
  '/app/settings': 'Settings',
  '/app/admin': 'User Management',
};

export default function Topbar() {
  const { pathname } = useLocation();
  const { toggleTheme } = useAuth();
  const label = BREADCRUMB_MAP[pathname] || 'Dashboard';

  return (
    <header className="app-topbar">
      <div className="breadcrumb">
        <span>Portal</span>
        <span>&rsaquo;</span>
        <span className="breadcrumb-active">{label}</span>
      </div>
      <div className="topbar-actions">
        <span className="demo-badge-floating">System Running in DEMO MODE</span>
        <button id="theme-toggle-btn" className="theme-toggle" title="Switch Theme Mode" onClick={toggleTheme}>
          <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        </button>
      </div>
    </header>
  );
}
