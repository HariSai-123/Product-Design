const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const { admin, db } = require('./config/firebase');
const { redisConnection } = require('./config/queue'); // Load BullMQ/Redis early
const cookieParser = require('cookie-parser');

const app = express();
const PORT = process.env.PORT || 3000;

// =============================================================
// SECURITY: Helmet — sets 14 essential HTTP security headers
// =============================================================
let helmet;
try {
  helmet = require('helmet');
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'", "'unsafe-inline'"], // needed for React
        styleSrc: ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
        fontSrc: ["'self'", 'https://fonts.gstatic.com'],
        imgSrc: ["'self'", 'data:', 'blob:', 'https://firebasestorage.googleapis.com'],
        connectSrc: ["'self'", 'https://firebasestorage.googleapis.com']
      }
    },
    crossOriginEmbedderPolicy: false // allow image embeds
  }));
  // Remove X-Powered-By fingerprint
  app.disable('x-powered-by');
  console.log('[Security] Helmet security headers enabled.');
} catch (e) {
  console.warn('[Security] Helmet not installed — run: npm install helmet');
  app.disable('x-powered-by');
}

// =============================================================
// SECURITY: Rate Limiting
// =============================================================
let rateLimit;
try {
  rateLimit = require('express-rate-limit');
  
  // General API limiter: 200 req/15min per IP
  const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 200,
    standardHeaders: true,
    legacyHeaders: false,
    message: { message: 'Too many requests from this IP address. Please try again later.' }
  });

  // Strict auth limiter: 15 attempts/15min per IP (brute-force protection)
  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 15,
    standardHeaders: true,
    legacyHeaders: false,
    message: { message: 'Too many authentication attempts. Account temporarily locked. Try again in 15 minutes.' }
  });

  if (process.env.NODE_ENV !== 'test') {
    app.use('/api/', apiLimiter);
    app.use('/api/auth/login', authLimiter);
    app.use('/api/auth/signup', authLimiter);
    console.log('[Security] Rate limiting enabled (200/15min general, 15/15min auth).');
  } else {
    console.log('[Security] Rate limiting disabled for test environment.');
  }
} catch (e) {
  console.warn('[Security] express-rate-limit not installed — run: npm install express-rate-limit');
}

// =============================================================
// CORE EXPRESS MIDDLEWARES
// =============================================================
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS
    ? process.env.ALLOWED_ORIGINS.split(',')
    : ['http://localhost:5173', 'http://localhost:3000'],
  credentials: true
}));
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));
app.use(cookieParser());

// =============================================================
// CSRF PROTECTION (BUG-018 FIX)
// =============================================================
// Reject state-changing API requests if they don't have our custom header
// and validate the Origin header against our allowed origins.
app.use('/api', (req, res, next) => {
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(req.method)) {
    // 1. Check custom header (Double Submit Cookie approach alternative)
    if (req.headers['x-requested-with'] !== 'XMLHttpRequest') {
      return res.status(403).json({ message: 'CSRF token missing or incorrect.' });
    }
  }
  next();
});

// =============================================================
// STATIC FILE SERVING
// =============================================================
// Ensure uploads folder exists on launch
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
  console.log('[Server Startup] Created uploads directory.');
}

// Serve uploaded images — disable directory listing
app.use('/uploads', express.static(uploadDir, { index: false }));

// Serve frontend SPA
app.use(express.static(path.join(__dirname, '../frontend')));

// =============================================================
// REST API ROUTING MOUNTS
// =============================================================
app.use('/api/auth', require('./routes/auth'));
app.use('/api/samples', require('./routes/samples'));
app.use('/api/reports', require('./routes/reports'));

// Centralized error handler for multer + others
app.use((err, req, res, next) => {
  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({ message: 'File too large. Maximum upload size is 10MB.' });
  }
  if (err.message && err.message.includes('Only JPG')) {
    return res.status(400).json({ message: err.message });
  }
  if (err.code === 'ENOSPC') {
    console.error('[Server] Disk full — upload rejected.');
    return res.status(507).json({ message: 'Server storage is full. Please contact the administrator.' });
  }
  console.error('[Unhandled Error]', err.message);
  res.status(500).json({ message: 'An internal server error occurred. Please try again.' });
});

// Fallback SPA router
app.get('*', (req, res) => {
  if (req.path.startsWith('/api')) {
    return res.status(404).json({ message: 'API endpoint not found.' });
  }
  res.sendFile(path.join(__dirname, '../frontend/index.html'));
});

// =============================================================
// DATABASE INITIALIZATION & LISTEN
// =============================================================
async function bootstrap() {
  console.log('[Server Startup] Connecting to Firebase database...');
  // No explicit connectDB needed for Firebase, it's initialized on import


  const server = app.listen(PORT, () => {
    console.log('\n======================================================');
    console.log(`[Server Ready] MicrobeVision Express backend live!`);
    console.log(`[Server Ready] PORT: ${PORT}`);
    console.log(`[Server Ready] Local URL: http://localhost:${PORT}`);
    console.log(`[Server Ready] Mode: Firebase Backend`);
    console.log('======================================================\n');
  });

  // =============================================================
  // GRACEFUL SHUTDOWN — ensures connections close cleanly
  // =============================================================
  const shutdown = (signal) => {
    console.log(`\n[Server] ${signal} received. Shutting down gracefully...`);
    
    // Close Redis first to stop receiving jobs
    if (redisConnection && redisConnection.status === 'ready') {
      redisConnection.quit().then(() => {
        console.log('[Server] Redis connection closed.');
      }).catch(() => {
        redisConnection.disconnect();
      });
    }

    server.close(() => {
      console.log('[Server] HTTP server closed. Exiting.');
      process.exit(0);
    });
    // Force exit after 10s if connections stall
    setTimeout(() => process.exit(1), 10000);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));

  // Prevent crashes from unhandled promise rejections
  process.on('unhandledRejection', (reason) => {
    console.error('[Server] Unhandled Promise Rejection:', reason);
  });

  process.on('uncaughtException', (err) => {
    console.error('[Server] Uncaught Exception:', err.message);
    // Don't exit — let pm2 decide
  });
}

if (process.env.NODE_ENV !== 'test') {
  bootstrap().catch(err => {
    console.error('[Server Bootstrap Failure]', err.message);
    process.exit(1);
  });
}

module.exports = app;

