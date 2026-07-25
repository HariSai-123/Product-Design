const express = require('express');
const router = express.Router();
const { auth, db } = require('../config/firebase');
const { authenticateJWT, authorizeRoles } = require('../middleware/auth');

function sanitizeInput(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');
}

// Note: Login, Signup, Forgot Password, Reset Password, and Logout
// are now handled directly by the Firebase Client SDK on the frontend.
// The backend only provides profile and admin endpoints.

// -------------------------------------------------------------
// POST /api/auth/register-profile
// Called after Firebase client signup to store user metadata in Firestore
// -------------------------------------------------------------
router.post('/register-profile', authenticateJWT, async (req, res) => {
  const { name, department, role } = req.body;
  const uid = req.user.id;
  
  try {
    await db.collection('users').doc(uid).set({
      name: name ? sanitizeInput(name) : 'New User',
      email: req.user.email,
      department: department ? sanitizeInput(department) : 'General Microbiology',
      role: role || 'Lab Technician',
      createdAt: new Date().toISOString()
    });
    
    res.status(201).json({ message: 'Profile created in Firestore.' });
  } catch (error) {
    console.error('Register profile error:', error);
    res.status(500).json({ message: 'Error creating user profile in DB.' });
  }
});

// -------------------------------------------------------------
// GET /api/auth/profile
// -------------------------------------------------------------
router.get('/profile', authenticateJWT, async (req, res) => {
  res.json({ user: req.user });
});

// -------------------------------------------------------------
// POST /api/auth/profile/update
// -------------------------------------------------------------
router.post('/profile/update', authenticateJWT, async (req, res) => {
  const { name, department } = req.body;
  
  try {
    const sanitizedName = name ? sanitizeInput(name) : undefined;
    const sanitizedDept = department ? sanitizeInput(department) : undefined;
    const updates = {};
    if (sanitizedName) updates.name = sanitizedName;
    if (sanitizedDept) updates.department = sanitizedDept;

    await db.collection('users').doc(req.user.id).update(updates);
    
    res.json({
      message: 'Profile updated successfully',
      user: { ...req.user, ...updates }
    });
  } catch (error) {
    console.error('Profile update error:', error);
    res.status(500).json({ message: 'Internal server error updating profile.' });
  }
});

// -------------------------------------------------------------
// GET /api/auth/admin/users
// -------------------------------------------------------------
router.get('/admin/users', authenticateJWT, authorizeRoles('Admin'), async (req, res) => {
  try {
    const snapshot = await db.collection('users').get();
    const users = [];
    snapshot.forEach(doc => {
      users.push({ id: doc.id, ...doc.data() });
    });
    res.json({ users });
  } catch (error) {
    console.error('Admin get users error:', error);
    res.status(500).json({ message: 'Internal server error fetching users list.' });
  }
});

// -------------------------------------------------------------
// POST /api/auth/admin/users/:id/role
// -------------------------------------------------------------
router.post('/admin/users/:id/role', authenticateJWT, authorizeRoles('Admin'), async (req, res) => {
  const { role } = req.body;
  const targetId = req.params.id;
  
  if (!role) {
    return res.status(400).json({ message: 'Role is required.' });
  }
  
  try {
    await db.collection('users').doc(targetId).update({ role });
    res.json({ message: `User role updated to ${role}` });
  } catch (error) {
    console.error('Admin edit role error:', error);
    res.status(500).json({ message: 'Internal server error updating user role.' });
  }
});

// -------------------------------------------------------------
// DELETE /api/auth/admin/users/:id
// -------------------------------------------------------------
router.delete('/admin/users/:id', authenticateJWT, authorizeRoles('Admin'), async (req, res) => {
  const targetId = req.params.id;

  if (targetId === req.user.id) {
    return res.status(400).json({ message: 'Administrators cannot revoke their own account. Assign another admin first.' });
  }

  try {
    // Delete from Auth
    await auth.deleteUser(targetId);
    // Delete from Firestore
    await db.collection('users').doc(targetId).delete();
    
    res.json({ message: 'User account revoked successfully.' });
  } catch (error) {
    console.error('Admin delete user error:', error);
    res.status(500).json({ message: 'Internal server error deleting user.' });
  }
});

// -------------------------------------------------------------
// POST /api/auth/settings/update
// -------------------------------------------------------------
router.post('/settings/update', authenticateJWT, async (req, res) => {
  const { twoFactorEnabled, reportingPreference, newPassword, currentPassword } = req.body;
  
  try {
    const updates = {};
    if (twoFactorEnabled !== undefined) updates.twoFactorEnabled = Boolean(twoFactorEnabled);
    if (reportingPreference) updates.reportingPreference = reportingPreference;
    
    if (Object.keys(updates).length > 0) {
      await db.collection('users').doc(req.user.id).update(updates);
    }
    
    // Note: Password changes are handled client-side via Firebase Auth SDK.
    // The backend only persists UI preferences to Firestore.
    res.json({ message: 'Settings updated successfully.' });
  } catch (error) {
    console.error('Settings update error:', error);
    res.status(500).json({ message: 'Internal server error updating settings.' });
  }
});

module.exports = router;
