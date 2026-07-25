import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiForgotPassword, apiResetPassword } from '../services/api';

export default function ForgotPage() {
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  const handleSendToken = async (e) => {
    e.preventDefault();
    if (!email) return;
    setLoading(true);
    setError('');
    try {
      const res = await apiForgotPassword(email);
      setMessage(res.message);
      setStep(2);
    } catch (err) {
      setError(err.message || 'Failed to send recovery token.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!token || !newPassword) return;
    if (newPassword.trim().length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await apiResetPassword(token, newPassword);
      setMessage(res.message);
      setStep(3);
    } catch (err) {
      setError(err.message || 'Failed to reset password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-view" style={{ display: 'block' }}>
      <div className="auth-wrapper">
        <div className="auth-branding">
          <div className="auth-branding-logo"><div className="logo-icon">M</div><span>MicrobeVision AI</span></div>
          <div className="auth-branding-quote">
            <h2>Clinical Verification Engine</h2>
            <p>Re-establish secure operator session via verified recovery code verification.</p>
          </div>
          <div className="auth-branding-footer">System Port: 3000 | Operational Environment: HIPAA Secure</div>
        </div>
        <div className="auth-form-container">
          <div className="glass-card auth-card">
            <h2>Recover Session</h2>
            <p className="auth-card-subtitle">
              {step === 1 && 'Enter your registered email address to receive password reset tokens.'}
              {step === 2 && 'Enter the reset token sent to your email and a new password.'}
              {step === 3 && 'Recovery complete.'}
            </p>
            
            {error && <div className="badge badge-critical" style={{ display: 'block', width: '100%', padding: 12, marginBottom: 20, borderRadius: 6, fontWeight: 600 }}>{error}</div>}
            
            {step === 1 && (
              <form onSubmit={handleSendToken}>
                <div className="form-group" style={{ marginBottom: 24 }}>
                  <label htmlFor="forgot-email">Laboratory Email Address</label>
                  <input type="email" id="forgot-email" className="form-input" placeholder="e.g. researcher@clinic.org" required autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} disabled={loading} />
                </div>
                <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                  {loading ? 'Transmitting...' : 'Transmit Security Token'}
                </button>
              </form>
            )}

            {step === 2 && (
              <form onSubmit={handleResetPassword}>
                {message && <div className="badge badge-low" style={{ display: 'block', width: '100%', padding: 12, marginBottom: 20, borderRadius: 6, fontWeight: 600 }}>{message}</div>}
                <div className="form-group" style={{ marginBottom: 15 }}>
                  <label htmlFor="reset-token">Recovery Token</label>
                  <input type="text" id="reset-token" className="form-input" placeholder="Paste token here" required value={token} onChange={e => setToken(e.target.value.trim())} disabled={loading} />
                </div>
                <div className="form-group" style={{ marginBottom: 24 }}>
                  <label htmlFor="new-password">New Password</label>
                  <input type="password" id="new-password" className="form-input" placeholder="••••••••••••" required minLength="8" value={newPassword} onChange={e => setNewPassword(e.target.value.replace(/\s/g, ''))} disabled={loading} />
                </div>
                <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                  {loading ? 'Verifying...' : 'Reset Password'}
                </button>
              </form>
            )}

            {step === 3 && (
              <div style={{ textAlign: 'center' }}>
                <div className="badge badge-low" style={{ display: 'block', padding: 12, borderRadius: 6, fontWeight: 600, textAlign: 'center', marginBottom: 20 }}>
                  {message}
                </div>
                <button className="btn btn-primary" onClick={() => navigate('/login')}>
                  Return to Login
                </button>
              </div>
            )}

            {step !== 3 && (
              <div style={{ marginTop: 30, textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)' }}>
                Return to <Link to="/login" className="auth-link">Login Shield</Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
