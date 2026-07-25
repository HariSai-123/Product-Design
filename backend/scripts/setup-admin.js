require('dotenv').config();
const admin = require('firebase-admin');
const { cert, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');

const targetEmail = process.argv[2];

if (!targetEmail) {
  console.error('Usage: node scripts/setup-admin.js <user-email>');
  process.exit(1);
}

// Initialize Admin SDK
let credential = null;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  credential = cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT));
} else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  try {
    const serviceAccount = require(`../${process.env.GOOGLE_APPLICATION_CREDENTIALS.replace('./', '')}`);
    credential = cert(serviceAccount);
  } catch(e) {
    credential = applicationDefault();
  }
} else {
  console.error('ERROR: No Firebase credentials found.');
  process.exit(1);
}

admin.initializeApp({
  credential,
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET || ''
});

const db = getFirestore();
const authAdmin = getAuth();

async function promoteToAdmin(email) {
  console.log(`\nLooking up user: ${email}`);

  let userRecord;
  try {
    userRecord = await authAdmin.getUserByEmail(email);
    console.log(`Found Firebase Auth user: ${userRecord.uid}`);
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      console.error(`ERROR: No Firebase Auth account found for ${email}`);
    } else {
      console.error('Firebase Auth error:', err.message);
    }
    process.exit(1);
  }

  const uid = userRecord.uid;

  await db.collection('users').doc(uid).set({
    name: userRecord.displayName || email.split('@')[0],
    email: email,
    role: 'Admin',
    department: 'Administration',
    createdAt: new Date().toISOString(),
    promotedToAdminAt: new Date().toISOString()
  }, { merge: true });

  console.log(`\n✅ SUCCESS: ${email} is now an Admin in Firestore.`);
  process.exit(0);
}

promoteToAdmin(targetEmail).catch(err => {
  console.error('Unexpected error:', err);
  process.exit(1);
});
