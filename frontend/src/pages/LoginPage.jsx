import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiLogin } from '../services/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handlePasswordChange = (e) => {
    setPassword(e.target.value.replace(/\s/g, ''));
  };

  const isValidEmail = (email) => {
    return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!isValidEmail(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }

    setLoading(true);
    try {
      // apiLogin calls Firebase signInWithEmailAndPassword.
      // onAuthStateChanged in AuthContext will detect the login and update user state.
      // We just navigate after successful Firebase sign-in.
      await apiLogin(email.trim(), password);
      navigate('/app/dashboard');
    } catch (err) {
      // Map Firebase error codes to user-friendly messages
      const msg = err.message || '';
      if (msg.includes('user-not-found') || msg.includes('wrong-password') || msg.includes('invalid-credential')) {
        setError('Incorrect email or password. Please try again.');
      } else if (msg.includes('too-many-requests')) {
        setError('Too many failed attempts. Please wait a moment before trying again.');
      } else if (msg.includes('user-disabled')) {
        setError('This account has been disabled. Contact your administrator.');
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-view" style={{ display: 'block' }}>
      <div className="auth-wrapper">
        <div className="auth-branding">
          <div className="auth-branding-logo">
            <div className="logo-icon">M</div>
            <span>MicrobeVision AI</span>
          </div>
          <div className="auth-branding-quote">
            <h2>Clinical Verification Engine</h2>
            <p>Secure login with standard laboratory authentication frameworks.</p>
          </div>
          <div className="auth-branding-footer">Operational Environment: HIPAA Secure | Firebase Auth</div>
        </div>
        <div className="auth-form-container">
          <div className="glass-card auth-card">
            <h2>Sign In</h2>
            <p className="auth-card-subtitle">Access your laboratory quality assurance account.</p>
            {error && <div className="badge badge-critical" style={{ display: 'block', width: '100%', padding: 12, marginBottom: 20, borderRadius: 6, fontWeight: 600 }}>{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="login-email">Laboratory Email Address</label>
                <input type="email" id="login-email" className="form-input" placeholder="e.g. researcher@hospital.org" required autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} />
              </div>
              <div className="form-group" style={{ marginBottom: 10 }}>
                <label htmlFor="login-password">Laboratory Password</label>
                <input type="password" id="login-password" className="form-input" placeholder="••••••••••••" required autoComplete="current-password" value={password} onChange={handlePasswordChange} />
              </div>
              <div className="auth-links" style={{ marginBottom: 24 }}>
                <Link to="/forgot" className="auth-link">Forgot Password?</Link>
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Verifying...' : 'Verify Credentials & Access'}
              </button>
            </form>
            <div style={{ marginTop: 30, textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)' }}>
              New operator? <Link to="/signup" className="auth-link">Register Lab Account</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
