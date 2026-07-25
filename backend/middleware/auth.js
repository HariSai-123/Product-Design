const { auth, db } = require('../config/firebase');

async function authenticateJWT(req, res, next) {
  let token;
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    token = authHeader.split(' ')[1];
  } else if (req.cookies && req.cookies.jwt) {
    token = req.cookies.jwt;
  }

  if (!token) {
    return res.status(401).json({ message: 'Access denied. Authentication token is missing.' });
  }

  try {
    const decodedToken = await auth.verifyIdToken(token);
    
    // Fetch custom claims or user doc from Firestore for role and department
    const userDoc = await db.collection('users').doc(decodedToken.uid).get();
    
    if (userDoc.exists) {
      req.user = {
        id: decodedToken.uid,
        email: decodedToken.email,
        ...userDoc.data()
      };
    } else {
      // Basic fallback if user not in Firestore
      req.user = {
        id: decodedToken.uid,
        email: decodedToken.email,
        role: decodedToken.role || 'Lab Technician',
        name: decodedToken.name || 'User'
      };
    }
    
    next();
  } catch (error) {
    console.error('Firebase Auth Error:', error);
    res.status(401).json({ message: 'Invalid or expired authentication token.' });
  }
}

function authorizeRoles(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        message: `Access denied. Required role: ${allowedRoles.join(' or ')}. Your role: ${req.user?.role || 'none'}.`
      });
    }
    next();
  };
}

module.exports = { authenticateJWT, authorizeRoles };
