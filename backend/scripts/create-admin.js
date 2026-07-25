require('dotenv').config();
const admin = require('firebase-admin');
const { cert, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');

const email = process.argv[2] || 'admin@microbevision.com';
const password = process.argv[3] || 'AdminPassword123!';

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

async function createAdminUser() {
  console.log(`\nCreating or finding admin user: ${email}`);

  let userRecord;
  try {
    userRecord = await authAdmin.getUserByEmail(email);
    console.log(`Firebase Auth user already exists: ${userRecord.uid}`);
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      console.log(`User not found. Creating user in Firebase Auth...`);
      userRecord = await authAdmin.createUser({
        email: email,
        password: password,
        displayName: 'System Admin',
        emailVerified: true
      });
      console.log(`Successfully created user: ${userRecord.uid}`);
    } else {
      console.error('Firebase Auth error:', err.message);
      process.exit(1);
    }
  }

  const uid = userRecord.uid;

  await db.collection('users').doc(uid).set({
    name: 'System Admin',
    email: email,
    role: 'Admin',
    department: 'Administration',
    createdAt: new Date().toISOString(),
    promotedToAdminAt: new Date().toISOString()
  }, { merge: true });

  console.log(`\n✅ SUCCESS: Admin user ${email} is fully configured.`);
  console.log(`Email: ${email}`);
  console.log(`Password: ${password}`);
  process.exit(0);
}

createAdminUser().catch(err => {
  console.error('Unexpected error:', err);
  process.exit(1);
});
