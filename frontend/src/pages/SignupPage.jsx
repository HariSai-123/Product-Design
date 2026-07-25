import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiSignup } from '../services/api';

export default function SignupPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'Lab Technician', department: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    let value = e.target.value;
    if (e.target.id === 'signup-password') {
      value = value.replace(/\s/g, '');
    }
    setForm({ ...form, [e.target.id.replace('signup-', '')]: value });
  };

  const isValidEmail = (email) => {
    return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
  };

  const isValidPassword = (password) => {
    if (password.length < 8) return false;
    if (!/(?=.*[a-z])/.test(password)) return false;
    if (!/(?=.*[A-Z])/.test(password)) return false;
    if (!/(?=.*\d)/.test(password)) return false;
    if (!/(?=.*[^A-Za-z0-9])/.test(password)) return false;
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.name.trim()) {
      setError('Please enter your full name.');
      return;
    }

    if (!isValidEmail(form.email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }

    if (!isValidPassword(form.password)) {
      setError('Password must be at least 8 characters with uppercase, lowercase, number, and special character.');
      return;
    }

    setLoading(true);
    try {
      // apiSignup calls Firebase createUserWithEmailAndPassword, then registers
      // the profile metadata (name, role, department) in Firestore via the backend.
      // onAuthStateChanged in AuthContext will detect the new user and update state.
      await apiSignup(form.name.trim(), form.email.trim(), form.password, form.role, form.department.trim());
      navigate('/app/dashboard');
    } catch (err) {
      const msg = err.message || '';
      if (msg.includes('email-already-in-use')) {
        setError('This email is already registered. Please sign in instead.');
      } else if (msg.includes('weak-password')) {
        setError('Password is too weak. Use at least 8 characters with mixed case, numbers, and symbols.');
      } else if (msg.includes('invalid-email')) {
        setError('The email address is invalid.');
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
          <div className="auth-branding-logo"><div className="logo-icon">M</div><span>MicrobeVision AI</span></div>
          <div className="auth-branding-quote">
            <h2>Clinical Verification Engine</h2>
            <p>Register new operators, assign functional roles, and connect to diagnostic nodes.</p>
          </div>
          <div className="auth-branding-footer">Operational Environment: HIPAA Secure | Firebase Auth</div>
        </div>
        <div className="auth-form-container">
          <div className="glass-card auth-card">
            <h2>Register Account</h2>
            <p className="auth-card-subtitle">Create active diagnostic operator credentials.</p>
            {error && <div className="badge badge-critical" style={{ display: 'block', width: '100%', padding: 12, marginBottom: 20, borderRadius: 6, fontWeight: 600 }}>{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="signup-name">Full Name</label>
                <input type="text" id="signup-name" className="form-input" placeholder="e.g. Dr. Jane Doe" required autoComplete="name" value={form.name} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label htmlFor="signup-email">Laboratory Email</label>
                <input type="email" id="signup-email" className="form-input" placeholder="e.g. j.doe@clinic.org" required autoComplete="email" value={form.email} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label htmlFor="signup-password">Choose Password</label>
                <input type="password" id="signup-password" className="form-input" placeholder="Min. 8 chars, mixed case, number & symbol" required autoComplete="new-password" value={form.password} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label htmlFor="signup-role">Assigned Laboratory Role</label>
                <select id="signup-role" className="form-input" value={form.role} onChange={handleChange}>
                  <option value="Lab Technician">Lab Technician (Upload & Run)</option>
                  <option value="Researcher">Researcher (Full Access & Export)</option>
                  <option value="Admin">Administrator (User Manager)</option>
                </select>
              </div>
              <div className="form-group" style={{ marginBottom: 24 }}>
                <label htmlFor="signup-department">Laboratory Department</label>
                <input type="text" id="signup-department" className="form-input" placeholder="e.g. Quality Control, Bio-Safety" value={form.department} onChange={handleChange} />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Registering...' : 'Register & Establish Session'}
              </button>
            </form>
            <div style={{ marginTop: 30, textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)' }}>
              Already registered? <Link to="/login" className="auth-link">Sign In Instead</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
