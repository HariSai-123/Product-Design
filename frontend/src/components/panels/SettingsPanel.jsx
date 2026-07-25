import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiUpdateProfile, apiUpdateSettings } from '../../services/api';

// ---- Local settings persistence helpers ----
const SETTINGS_KEY = 'mv_ui_settings';
function loadUISettings() {
  try { return JSON.parse(localStorage.getItem(SETTINGS_KEY)) || {}; } catch { return {}; }
}
function saveUISettings(updates) {
  const current = loadUISettings();
  localStorage.setItem(SETTINGS_KEY, JSON.stringify({ ...current, ...updates }));
}

export default function SettingsPanel({ showNotification }) {
  const { user, updateUser } = useAuth();

  // Profile form
  const [profileForm, setProfileForm] = useState({
    name: user?.name || '',
    department: user?.department || ''
  });

  // Security / preferences form — also restores from localStorage (BUG-009 FIX)
  const saved = loadUISettings();
  const [secForm, setSecForm] = useState({
    reportingPreference: saved.reportingPreference || user?.reportingPreference || 'Detailed',
    twoFactorEnabled: saved.twoFactorEnabled !== undefined ? saved.twoFactorEnabled : (user?.twoFactorEnabled || false),
    currentPassword: '',
    newPassword: ''
  });

  const [saving1, setSaving1] = useState(false);
  const [saving2, setSaving2] = useState(false);

  // Sync form whenever the global user object changes (BUG-002 FIX)
  useEffect(() => {
    if (user) {
      setProfileForm({ name: user.name || '', department: user.department || '' });
      // Only update sec form from server if localStorage has no preference saved
      const localSettings = loadUISettings();
      setSecForm(prev => ({
        ...prev,
        reportingPreference: localSettings.reportingPreference || user.reportingPreference || 'Detailed',
        twoFactorEnabled: localSettings.twoFactorEnabled !== undefined ? localSettings.twoFactorEnabled : (user.twoFactorEnabled || false)
      }));
    }
  }, [user]);

  const handleProfileSave = async (e) => {
    e.preventDefault();
    if (!profileForm.name.trim()) {
      showNotification('danger', 'Name cannot be empty.');
      return;
    }
    setSaving1(true);
    try {
      const data = await apiUpdateProfile(profileForm.name.trim(), profileForm.department.trim());
      // BUG-002 FIX: Refresh global auth context + localStorage after profile update
      updateUser(data.user);
      showNotification('success', 'Profile updated successfully.');
    } catch (err) {
      showNotification('danger', err.message || 'Failed to update profile.');
    } finally {
      setSaving1(false);
    }
  };

  const handleSecSave = async (e) => {
    e.preventDefault();
    if (secForm.newPassword && secForm.newPassword.trim().length === 0) {
      showNotification('danger', 'Password cannot be empty or contain only spaces.');
      return;
    }
    if (secForm.newPassword && secForm.newPassword.length < 8) {
      showNotification('danger', 'New password must be at least 8 characters long.');
      return;
    }
    setSaving2(true);
    try {
      const payload = {
        twoFactorEnabled: secForm.twoFactorEnabled,
        reportingPreference: secForm.reportingPreference
      };
      if (secForm.newPassword) {
        payload.newPassword = secForm.newPassword;
        payload.currentPassword = secForm.currentPassword;
      }
      await apiUpdateSettings(payload);

      // BUG-009/BUG-003 FIX: Persist settings to localStorage so they survive page refresh
      saveUISettings({
        reportingPreference: secForm.reportingPreference,
        twoFactorEnabled: secForm.twoFactorEnabled
      });

      showNotification('success', 'Security settings saved and synced.');
      setSecForm(prev => ({ ...prev, currentPassword: '', newPassword: '' }));
    } catch (err) {
      showNotification('danger', err.message || 'Failed to update settings.');
    } finally {
      setSaving2(false);
    }
  };

  // Password strength indicator
  const getPasswordStrength = (pwd) => {
    if (!pwd) return null;
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    if (score <= 1) return { label: 'Weak', color: '#ef4444' };
    if (score === 2) return { label: 'Fair', color: '#f97316' };
    if (score === 3) return { label: 'Good', color: '#eab308' };
    return { label: 'Strong', color: '#10b981' };
  };
  const pwdStrength = getPasswordStrength(secForm.newPassword);

  return (
    <div id="settings-panel" className="panel-view active">
      <div className="panel-header">
        <h2>Laboratory Console Configurations</h2>
        <p>Manage pathology identity details, password credentials, and active 2FA modes.</p>
      </div>
      <div className="settings-grid">
        {/* Profile Section */}
        <form className="glass-card" onSubmit={handleProfileSave}>
          <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>Operator Profile Identity</h3>
          <div className="form-group">
            <label htmlFor="settings-profile-name">Full Credentials Title</label>
            <input
              type="text"
              id="settings-profile-name"
              className="form-input"
              required
              value={profileForm.name}
              onChange={e => setProfileForm({ ...profileForm, name: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label htmlFor="settings-profile-dept">Laboratory Department Branch</label>
            <input
              type="text"
              id="settings-profile-dept"
              className="form-input"
              value={profileForm.department}
              onChange={e => setProfileForm({ ...profileForm, department: e.target.value })}
            />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 10 }}
            disabled={saving1}
          >
            {saving1 ? 'Saving...' : 'Save Profile Changes'}
          </button>
        </form>

        {/* Security / Preferences Section */}
        <form
          className="glass-card"
          onSubmit={handleSecSave}
          style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
        >
          <div>
            <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>Diagnostic Preferences &amp; Security</h3>
            <div className="form-group">
              <label htmlFor="settings-report-preference">Default Exporter Formatting Template</label>
              <select
                id="settings-report-preference"
                className="form-input"
                value={secForm.reportingPreference}
                onChange={e => setSecForm({ ...secForm, reportingPreference: e.target.value })}
              >
                <option value="Simple">Simple Summary Log</option>
                <option value="Detailed">Standard Detailed Laboratory Report</option>
                <option value="Comprehensive">Comprehensive Diagnostic Certificate</option>
              </select>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '15px 0', borderTop: '1px solid var(--border-color)', borderBottom: '1px solid var(--border-color)', marginBottom: 20 }}>
              <div>
                <strong style={{ fontSize: 13, color: 'var(--text-primary)', display: 'block' }}>Enable Multi-Factor SMS Validation (2FA)</strong>
                <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Enforce verification code overlays at system access login.</span>
              </div>
              <input
                type="checkbox"
                id="settings-2fa-toggle"
                style={{ width: 20, height: 20, cursor: 'pointer' }}
                checked={secForm.twoFactorEnabled}
                onChange={e => setSecForm({ ...secForm, twoFactorEnabled: e.target.checked })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="settings-current-pwd">Current Password</label>
              <input
                type="password"
                id="settings-current-pwd"
                className="form-input"
                placeholder="••••••••••••"
                value={secForm.currentPassword}
                onChange={e => setSecForm({ ...secForm, currentPassword: e.target.value.replace(/\s/g, '') })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="settings-new-pwd">New Secure Password</label>
              <input
                type="password"
                id="settings-new-pwd"
                className="form-input"
                placeholder="Minimum 8 characters"
                value={secForm.newPassword}
                onChange={e => setSecForm({ ...secForm, newPassword: e.target.value.replace(/\s/g, '') })}
              />
              {/* BUG-010 FIX: Password strength indicator */}
              {pwdStrength && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6 }}>
                  <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--border-color)', overflow: 'hidden' }}>
                    <div style={{
                      height: '100%',
                      width: pwdStrength.label === 'Weak' ? '25%' : pwdStrength.label === 'Fair' ? '50%' : pwdStrength.label === 'Good' ? '75%' : '100%',
                      background: pwdStrength.color,
                      transition: 'width 0.3s ease, background 0.3s ease'
                    }} />
                  </div>
                  <span style={{ fontSize: 11, fontWeight: 600, color: pwdStrength.color }}>{pwdStrength.label}</span>
                </div>
              )}
            </div>
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 10 }}
            disabled={saving2}
          >
            {saving2 ? 'Syncing...' : 'Sync Security Configs'}
          </button>
        </form>
      </div>
    </div>
  );
}
