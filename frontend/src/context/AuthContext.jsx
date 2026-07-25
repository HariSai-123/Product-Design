import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { auth } from '../config/firebase';
import { onAuthStateChanged, signOut } from 'firebase/auth';

const AuthContext = createContext(null);
const THEME_KEY = 'mv_dark_theme';



export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const isDark = localStorage.getItem(THEME_KEY) === 'true';
    document.body.classList.toggle('dark-theme', isDark);

    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        try {
          const idToken = await firebaseUser.getIdToken();
          setToken(idToken);

          const res = await fetch('/api/auth/profile', {
            headers: { 'Authorization': `Bearer ${idToken}` }
          });
          
          if (!res.ok) {
            throw new Error(`Backend profile fetch failed with status: ${res.status}`);
          }
          
          const data = await res.json();
          const profileData = data.user;

          setUser({
            id: firebaseUser.uid,
            email: firebaseUser.email,
            name: profileData?.name || firebaseUser.email.split('@')[0],
            role: profileData?.role || 'Lab Technician',
            department: profileData?.department || '',
            ...(profileData || {})
          });
        } catch (err) {
          console.error('[AuthContext] Backend authentication/profile fetch failed:', err.message);
          // Crucial: We must sign out of Firebase if backend authentication fails
          // because the app expects a valid backend profile to function securely.
          await signOut(auth);
          setToken(null);
          setUser(null);
        }
      } else {
        setToken(null);
        setUser(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const logout = useCallback(async () => {
    await signOut(auth);
    // onAuthStateChanged fires automatically and clears state
  }, []);

  const toggleTheme = useCallback(() => {
    const isNowDark = document.body.classList.toggle('dark-theme');
    localStorage.setItem(THEME_KEY, String(isNowDark));
  }, []);

  const updateUser = useCallback((partialUser) => {
    setUser(prev => ({ ...prev, ...partialUser }));
  }, []);

  const refreshToken = useCallback(async () => {
    if (auth.currentUser) {
      const idToken = await auth.currentUser.getIdToken(true);
      setToken(idToken);
      return idToken;
    }
    return null;
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, logout, toggleTheme, updateUser, refreshToken, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
