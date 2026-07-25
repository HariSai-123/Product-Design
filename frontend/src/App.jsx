import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import ForgotPage from './pages/ForgotPage';
import AppDashboard from './pages/AppDashboard';

function ProtectedRoute({ children }) {
  const { user, token, loading } = useAuth();
  if (loading) return null;
  if (!user || !token) return <Navigate to="/login" replace />;
  return children;
}

function PublicRoute({ children }) {
  const { user, token, loading } = useAuth();
  if (loading) return null;
  if (user && token) return <Navigate to="/app/dashboard" replace />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<PublicRoute><LandingPage /></PublicRoute>} />
          <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
          <Route path="/signup" element={<PublicRoute><SignupPage /></PublicRoute>} />
          <Route path="/forgot" element={<PublicRoute><ForgotPage /></PublicRoute>} />
          <Route path="/app/*" element={<ProtectedRoute><AppDashboard /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
