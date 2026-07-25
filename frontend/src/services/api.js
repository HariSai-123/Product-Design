import { auth } from '../config/firebase';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut, 
  sendPasswordResetEmail 
} from 'firebase/auth';

const API_BASE = '/api';

export async function getFirebaseToken() {
  const user = auth.currentUser;
  if (user) {
    return await user.getIdToken();
  }
  return null;
}

// ---- Core Fetch Wrapper ----
// signal: optional AbortSignal for cancellation
export async function apiFetch(endpoint, options = {}, signal = null) {
  const token = await getFirebaseToken();
  const headers = { 
    'X-Requested-With': 'XMLHttpRequest',
    ...(options.headers || {}) 
  };

  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  } else if (!options.body) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const fetchOptions = {
    ...options,
    headers,
    credentials: 'omit',
    ...(signal ? { signal } : {})
  };

  const response = await fetch(`${API_BASE}${endpoint}`, fetchOptions);

  if (response.status === 401) {
    await signOut(auth);
    window.location.href = '/login';
    throw new Error('Session expired. Please log in again.');
  }

  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    if (!response.ok) throw new Error(`Server error: ${response.status}`);
    return {};
  }

  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Server error.');
  return data;
}

// ---- Auth API ----
export async function apiLogin(email, password) {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const token = await userCredential.user.getIdToken();
    return { token, user: { email: userCredential.user.email, id: userCredential.user.uid } };
  } catch (error) {
    throw new Error(error.message);
  }
}

export async function apiSignup(name, email, password, role, department) {
  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    
    // Register the extended profile in Firestore via backend.
    // This MUST succeed for the user to be fully functional.
    const idToken = await userCredential.user.getIdToken();
    const res = await fetch('/api/auth/register-profile', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${idToken}`,
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: JSON.stringify({ name, role, department })
    });
    
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      // Rollback Firebase user creation if backend fails
      await userCredential.user.delete();
      throw new Error(errData.message || `Backend profile creation failed: ${res.status}`);
    }
    
    return { user: { email: userCredential.user.email, id: userCredential.user.uid, name } };
  } catch (error) {
    throw new Error(error.message);
  }
}

export async function apiLogout() {
  try {
    await signOut(auth);
  } finally {
    window.location.href = '/';
  }
}

export async function apiForgotPassword(email) {
  try {
    await sendPasswordResetEmail(auth, email);
    return { message: 'Password reset email sent.' };
  } catch (error) {
    throw new Error(error.message);
  }
}

export async function apiResetPassword(token, newPassword) {
  // Not easily implemented via API without custom action URL handling in Firebase.
  // We assume the user clicks the link in their email instead.
  return { message: 'Please use the link sent to your email to reset your password.' };
}

// ---- Samples API ----
export async function apiGetSamples(params = {}) {
  const q = new URLSearchParams(params).toString();
  return apiFetch(`/samples${q ? '?' + q : ''}`);
}

export async function apiGetAdminHistory(params = {}) {
  const q = new URLSearchParams(params).toString();
  return apiFetch(`/samples/admin/history${q ? '?' + q : ''}`);
}

// signal: AbortSignal from AbortController — allows cancelling a stale upload
export async function apiUploadSample(formData, signal = null) {
  return apiFetch('/samples/upload', { method: 'POST', body: formData }, signal);
}

export async function apiUpdateDetections(sampleId, detections) {
  return apiFetch(`/samples/${sampleId}/update-detections`, {
    method: 'POST',
    body: JSON.stringify({ detections }),
  });
}

export async function apiDeleteSample(sampleId) {
  return apiFetch(`/samples/${sampleId}`, { method: 'DELETE' });
}

// ---- Reports API ----
export async function apiExportCSV() {
  const token = await getFirebaseToken();
  const res = await fetch('/api/reports/csv', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Failed to export CSV.');
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `MicrobeVision_Report_${Date.now()}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export async function apiGetPdfUrl(sampleId) {
  const token = await getFirebaseToken();
  return `/api/reports/pdf/${sampleId}?token=${encodeURIComponent(token)}`;
}

// ---- Admin API ----
export async function apiGetAdminUsers() {
  return apiFetch('/auth/admin/users');
}

export async function apiUpdateUserRole(userId, role) {
  return apiFetch(`/auth/admin/users/${userId}/role`, {
    method: 'POST',
    body: JSON.stringify({ role }),
  });
}

export async function apiDeleteUser(userId) {
  return apiFetch(`/auth/admin/users/${userId}`, { method: 'DELETE' });
}

// ---- Profile/Settings API ----
export async function apiUpdateProfile(name, department) {
  return apiFetch('/auth/profile/update', {
    method: 'POST',
    body: JSON.stringify({ name, department }),
  });
}

export async function apiUpdateSettings(payload) {
  return apiFetch('/auth/settings/update', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
