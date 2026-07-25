const admin = require('firebase-admin');
const { applicationDefault, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');
const { getAuth } = require('firebase-admin/auth');

let firebaseApp = null;
let db = null;
let storage = null;
let auth = null;

try {
  let credential = null;

  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    credential = cert(serviceAccount);
  } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    try {
      const serviceAccount = require(`../${process.env.GOOGLE_APPLICATION_CREDENTIALS.replace('./', '')}`);
      credential = cert(serviceAccount);
    } catch (e) {
      console.warn("Could not load service account from JSON, falling back to applicationDefault");
      credential = applicationDefault();
    }
  }

  if (credential) {
    firebaseApp = admin.initializeApp({
      credential,
      storageBucket: process.env.FIREBASE_STORAGE_BUCKET || ''
    });

    db = getFirestore();
    storage = getStorage();
    auth = getAuth();
    console.log('[Firebase] Admin SDK initialized successfully.');
  } else {
    console.warn('[Firebase] Warning: Neither FIREBASE_SERVICE_ACCOUNT nor GOOGLE_APPLICATION_CREDENTIALS found. Firebase Admin SDK NOT initialized.');
  }
} catch (error) {
  console.error('[Firebase] Failed to initialize Firebase Admin SDK:', error.message);
}

module.exports = { admin, db, storage, auth };
