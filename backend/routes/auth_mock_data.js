// Local in-memory store for Demo Mode
const bcrypt = require('bcryptjs');

const MOCK_USERS = [
  {
    _id: 'user-admin-101',
    name: 'Dr. Sarah Jenkins',
    email: 'admin@microbevision.com',
    password: '', // will be hashed on startup
    role: 'Admin',
    department: 'Pathology & QA',
    twoFactorEnabled: true,
    reportingPreference: 'Comprehensive',
    createdAt: new Date('2026-01-15T08:00:00Z')
  },
  {
    _id: 'user-researcher-102',
    name: 'Dr. Michael Chen',
    email: 'researcher@microbevision.com',
    password: '', // will be hashed on startup
    role: 'Researcher',
    department: 'Bio-Safety Research',
    twoFactorEnabled: false,
    reportingPreference: 'Detailed',
    createdAt: new Date('2026-02-10T09:30:00Z')
  },
  {
    _id: 'user-technician-103',
    name: 'Alex Rivera',
    email: 'tech@microbevision.com',
    password: '', // will be hashed on startup
    role: 'Lab Technician',
    department: 'Quality Control',
    twoFactorEnabled: false,
    reportingPreference: 'Simple',
    createdAt: new Date('2026-03-01T10:15:00Z')
  }
];

// Hash mock passwords asynchronously on startup
async function initializeMockPasswords() {
  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash('password123', salt);
  MOCK_USERS.forEach(u => {
    u.password = hash;
  });
  console.log('[Auth Mock] Hashed password "password123" for demo users.');
}

initializeMockPasswords();

module.exports = {
  MOCK_USERS
};
