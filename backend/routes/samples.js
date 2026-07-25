const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { db, storage } = require('../config/firebase');
const { authenticateJWT } = require('../middleware/auth');
const { enqueueAIAnalysis } = require('../config/queue');


let he;
try {
  he = require('he');
} catch (e) {
  he = { encode: (s) => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#x27;') };
}

function sanitize(str) {
  if (str === null || str === undefined) return '';
  return he.encode(String(str).trim().slice(0, 500));
}

const localStore = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadPath)) fs.mkdirSync(uploadPath, { recursive: true });
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({
  storage: localStore,
  fileFilter: (req, file, cb) => {
    const blockedTypes = /svg/;
    if (blockedTypes.test(path.extname(file.originalname).toLowerCase()) || blockedTypes.test(file.mimetype)) {
      return cb(new Error('SVG files are not allowed for security reasons.'));
    }
    const allowedTypes = /jpeg|jpg|png|tiff|tif/;
    if (allowedTypes.test(path.extname(file.originalname).toLowerCase()) && allowedTypes.test(file.mimetype)) {
      return cb(null, true);
    }
    cb(new Error('Only JPG, PNG, and TIFF images are supported.'));
  },
  limits: { fileSize: 10 * 1024 * 1024 }
});

function getRiskLevel(count) {
  if (count <= 5) return 'Low';
  if (count <= 25) return 'Medium';
  if (count <= 60) return 'High';
  return 'Critical';
}

function calculateZones(detections) {
  let aCount = 0, bCount = 0, cCount = 0, dCount = 0;
  detections.forEach(det => {
    if (det.x < 200) { if (det.y < 200) aCount++; else cCount++; } 
    else { if (det.y < 200) bCount++; else dCount++; }
  });
  const total = detections.length || 1;
  return [
    { name: 'Zone A (North-West)', count: aCount, density: parseFloat(((aCount/total)*100).toFixed(1)), risk: getRiskLevel(aCount * 2) },
    { name: 'Zone B (North-East)', count: bCount, density: parseFloat(((bCount/total)*100).toFixed(1)), risk: getRiskLevel(bCount * 2) },
    { name: 'Zone C (South-West)', count: cCount, density: parseFloat(((cCount/total)*100).toFixed(1)), risk: getRiskLevel(cCount * 2) },
    { name: 'Zone D (South-East)', count: dCount, density: parseFloat(((dCount/total)*100).toFixed(1)), risk: getRiskLevel(dCount * 2) }
  ];
}

// -------------------------------------------------------------
// GET /api/samples/admin/history
// -------------------------------------------------------------
router.get('/admin/history', authenticateJWT, async (req, res) => {
  if (req.user.role !== 'Admin') {
    return res.status(403).json({ message: 'Forbidden: Admin access required.' });
  }
  try {
    const snapshot = await db.collection('samples').orderBy('createdAt', 'desc').get();
    const samples = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.json({ samples });
  } catch (error) {
    console.error('Fetch admin history error:', error);
    if (error.code === 9) {
      console.error('[Firestore] MISSING COMPOSITE INDEX. Create it at:', error.details);
    }
    res.status(500).json({ message: 'Error retrieving global samples records.' });
  }
});

// -------------------------------------------------------------
// GET /api/samples
// -------------------------------------------------------------
router.get('/', authenticateJWT, async (req, res) => {
  const isAdmin = req.user.role === 'Admin';
  try {
    let snapshot;
    if (isAdmin) {
      // Admin: get all samples ordered by createdAt — no composite index needed
      snapshot = await db.collection('samples').orderBy('createdAt', 'desc').get();
    } else {
      // Regular user: filter by userId first, then sort in memory to avoid
      // requiring a composite Firestore index (userId + createdAt).
      // Once the composite index is created in Firebase Console, replace this
      // with: .where('userId','==',uid).orderBy('createdAt','desc').get()
      snapshot = await db.collection('samples').where('userId', '==', req.user.id).get();
    }
    let samples = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    // Sort newest first in memory (safe fallback while composite index is pending)
    samples.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    res.json({ samples });
  } catch (error) {
    console.error('Fetch samples error:', error);
    if (error.code === 9) {
      console.error('[Firestore] MISSING COMPOSITE INDEX. Create it at:', error.details);
    }
    res.status(500).json({ message: 'Error retrieving samples records. Check server logs for details.' });
  }
});

// -------------------------------------------------------------
// POST /api/samples/upload
// -------------------------------------------------------------
router.post('/upload', authenticateJWT, upload.single('sampleImage'), async (req, res) => {
  if (req.fileValidationError) return res.status(400).json({ message: req.fileValidationError });
  const { batchId, applianceType, dilutionFactor, operatorName, comments } = req.body;
  const safeBatchId = sanitize(batchId);
  const safeOperatorName = sanitize(operatorName);
  const safeComments = sanitize(comments);

  if (!safeBatchId || !applianceType || !safeOperatorName || !req.file) {
    if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
    return res.status(400).json({ message: 'Missing required fields or image.' });
  }

  const dilFactor = Math.max(1, parseFloat(dilutionFactor) || 1);
  const crypto = require('crypto');
  const sampleId = crypto.randomUUID();
  const uid = req.user.id;

  const fileBuffer = fs.readFileSync(req.file.path);
  const UPLOAD_HASH = crypto.createHash('sha256').update(fileBuffer).digest('hex');
  
  console.log(`\n[UPLOAD TRACE]
  requestId: ${req.headers['x-request-id'] || 'none'}
  sampleId: ${sampleId}
  uid: ${uid}
  originalName: ${req.file.originalname}
  fileSize: ${req.file.size}
  reqFilePath: ${req.file.path}
  UPLOAD_HASH: ${UPLOAD_HASH}\n`);

  // Rename original immediately to include sampleId and uuid for global uniqueness
  const ext = path.extname(req.file.originalname) || '.jpg';
  const fileUUID = crypto.randomUUID();
  const uniqueOriginalName = `${sampleId}-${fileUUID}-original${ext}`;
  const uniqueOriginalPath = path.join(path.dirname(req.file.path), uniqueOriginalName);
  
  try {
    fs.renameSync(req.file.path, uniqueOriginalPath);
  } catch (err) {
    console.error('[Storage] Error renaming original file:', err);
    return res.status(500).json({ message: 'Internal error handling uploaded file.' });
  }

  // Call AI Python Service with expected hash and updated path
  let aiResults = { colonyCount: 0, detections: [], zones: [], contaminationRisk: 'Low', processedImage: '' };
  try {
    const pythonServiceUrl = process.env.PYTHON_SERVICE_URL || 'http://localhost:5001/analyze';
    const response = await fetch(pythonServiceUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        image_path: uniqueOriginalPath, 
        appliance_type: applianceType,
        expected_hash: UPLOAD_HASH 
      })
    });
    if (response.ok) {
      const data = await response.json();
      aiResults = {
        colonyCount: data.colony_count,
        detections: data.detections,
        zones: calculateZones(data.detections),
        contaminationRisk: getRiskLevel(data.colony_count),
        processedImage: data.processed_image || ''
      };
    } else {
      const errText = await response.text();
      console.error('[AI Service Error]:', errText);
      return res.status(response.status).json({ message: `AI Service Error: ${errText}` });
    }
  } catch (err) {
    console.error('Python AI service unreachable:', err.message);
  }

  let originalImageUrl = `/uploads/${uniqueOriginalName}`;
  let processedImageUrl = '';

  if (aiResults.processedImage) {
    const processedPath = path.join(path.dirname(uniqueOriginalPath), aiResults.processedImage);
    if (fs.existsSync(processedPath)) {
      const uniqueProcessedName = `${sampleId}-${fileUUID}-processed.jpg`;
      const uniqueProcessedPath = path.join(path.dirname(processedPath), uniqueProcessedName);
      try {
        fs.renameSync(processedPath, uniqueProcessedPath);
        processedImageUrl = `/uploads/${uniqueProcessedName}`;
        const processedStats = fs.statSync(uniqueProcessedPath);
        console.log(`\n[OUTPUT TRACE]
  sampleId: ${sampleId}
  processedImagePath: ${uniqueProcessedPath}
  processedFileExists: true
  processedFileSize: ${processedStats.size}\n`);
      } catch (err) {
        console.error('[Storage] Error renaming processed file:', err);
        processedImageUrl = `/uploads/${aiResults.processedImage}`;
      }
    } else {
      console.log(`\n[OUTPUT TRACE]
  sampleId: ${sampleId}
  processedImagePath: ${processedPath}
  processedFileExists: false
  processedFileSize: 0\n`);
    }
  }

  try {
    const newSample = {
      sampleId: sampleId, // Expose explicitly as requested
      userId: uid,
      userName: req.user.name || 'User',
      email: req.user.email,
      role: req.user.role,
      department: req.user.department || 'General',
      batchId: safeBatchId,
      applianceType,
      dilutionFactor: dilFactor,
      operatorName: safeOperatorName,
      status: 'Completed',
      originalImageUrl,
      processedImageUrl,
      originalImageHash: UPLOAD_HASH,
      storageBackend: 'local',
      colonyCount: aiResults.colonyCount,
      cfuCount: aiResults.colonyCount * dilFactor,
      contaminationRisk: aiResults.contaminationRisk,
      detections: aiResults.detections,
      zones: aiResults.zones,
      comments: safeComments,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    // Use sampleId directly as the document ID
    const docRef = db.collection('samples').doc(sampleId);
    await docRef.set(newSample);
    
    // Read the document immediately back to verify data persistence as requested
    const savedDoc = await docRef.get();
    const savedData = savedDoc.data();
    
    console.log(`\n[FIRESTORE VERIFY]
  documentId: ${savedDoc.id}
  sampleIdMatch: ${savedData.sampleId === sampleId}
  originalImageMatch: ${savedData.originalImageUrl === originalImageUrl}
  processedImageMatch: ${savedData.processedImageUrl === processedImageUrl}\n`);

    if (savedData.sampleId !== sampleId || savedData.originalImageUrl !== originalImageUrl) {
      throw new Error("Critical Firestore data persistence mismatch.");
    }

    const sampleObj = { id: savedDoc.id, ...savedData };

    res.status(202).json({
      message: 'Sample uploaded and analyzed successfully.',
      sample: sampleObj
    });
  } catch (error) {
    console.error('[Firestore] Save sample error:', error);
    res.status(500).json({ message: `Internal server error saving sample record: ${error.message}` });
  }
});

// -------------------------------------------------------------
// POST /api/samples/:id/update-detections
// -------------------------------------------------------------
router.post('/:id/update-detections', authenticateJWT, async (req, res) => {
  const { detections } = req.body;
  const sampleId = req.params.id;
  
  if (!Array.isArray(detections)) return res.status(400).json({ message: 'Detections array is required.' });
  
  try {
    const docRef = db.collection('samples').doc(sampleId);
    const doc = await docRef.get();
    if (!doc.exists || (doc.data().userId !== req.user.id && req.user.role !== 'Admin')) {
      return res.status(404).json({ message: 'Sample record not found or access denied.' });
    }
    
    const updatedCount = detections.length;
    const sampleData = doc.data();
    
    const updates = {
      detections,
      colonyCount: updatedCount,
      cfuCount: updatedCount * sampleData.dilutionFactor,
      contaminationRisk: getRiskLevel(updatedCount),
      zones: calculateZones(detections)
    };
    
    await docRef.update(updates);
    res.json({ message: 'Colony detections updated successfully', sample: { id: sampleId, ...sampleData, ...updates } });
  } catch (error) {
    console.error('Update detections error:', error);
    res.status(500).json({ message: 'Error updating manual colony annotations.' });
  }
});

// -------------------------------------------------------------
// DELETE /api/samples/:id
// -------------------------------------------------------------
router.delete('/:id', authenticateJWT, async (req, res) => {
  try {
    const docRef = db.collection('samples').doc(req.params.id);
    const doc = await docRef.get();
    if (!doc.exists || (doc.data().userId !== req.user.id && req.user.role !== 'Admin')) {
      return res.status(404).json({ message: 'Sample record not found or access denied.' });
    }
    await docRef.delete();
    res.json({ message: 'Sample record deleted successfully.' });
  } catch (error) {
    console.error('Delete sample error:', error);
    res.status(500).json({ message: 'Error deleting sample record.' });
  }
});

module.exports = router;
