import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

// Validate that all required Firebase config values are loaded from .env
// This detects missing/undefined values without logging sensitive keys.
const requiredVars = [
  'VITE_FIREBASE_API_KEY',
  'VITE_FIREBASE_AUTH_DOMAIN',
  'VITE_FIREBASE_PROJECT_ID',
  'VITE_FIREBASE_STORAGE_BUCKET',
  'VITE_FIREBASE_MESSAGING_SENDER_ID',
  'VITE_FIREBASE_APP_ID'
];

const missing = requiredVars.filter(key => {
  const val = import.meta.env[key];
  return !val || val.startsWith('YOUR_');
});

if (missing.length > 0) {
  console.error('[Firebase] Missing or invalid environment variables:', missing);
  console.error('[Firebase] Ensure frontend/.env exists and has been loaded by Vite (restart dev server after adding/editing .env).');
}

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
