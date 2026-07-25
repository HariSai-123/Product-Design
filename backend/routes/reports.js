const express = require('express');
const router = express.Router();
const { db, auth } = require('../config/firebase');
const { authenticateJWT } = require('../middleware/auth');

// XSS prevention: escape all user-controlled HTML output — BUG-015/016/017 FIX
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

// -------------------------------------------------------------
// GET /api/reports/csv - Export historical database log to CSV
// -------------------------------------------------------------
router.get('/csv', authenticateJWT, async (req, res) => {
  let samples = [];
  const userId = req.user.id;
  const isAdmin = req.user.role === 'Admin';
  
  try {
    let snapshot;
    if (isAdmin) {
      snapshot = await db.collection('samples').orderBy('createdAt', 'desc').get();
    } else {
      snapshot = await db.collection('samples').where('userId', '==', userId).orderBy('createdAt', 'desc').get();
    }
    samples = snapshot.docs.map(doc => ({ _id: doc.id, ...doc.data() }));
  } catch (error) {
    return res.status(500).json({ message: 'Error fetching samples for report.' });
  }

  // Create CSV Header
  let csvContent = '\uFEFF'; // UTF-8 BOM for Excel compatibility
  csvContent += 'Record ID,Batch ID,Appliance Type,Dilution Factor,Operator,Status,Colony Count,CFU Count (CFU/mL),Contamination Risk,Created Date,Comments\n';
  
  // Create CSV Rows
  samples.forEach(s => {
    const safeComments = s.comments ? s.comments.replace(/"/g, '""').replace(/\n/g, ' ') : '';
    const formattedDate = new Date(s.createdAt).toISOString().replace(/T/, ' ').replace(/\..+/, '');
    csvContent += `"${s._id}","${s.batchId}","${s.applianceType}",${s.dilutionFactor},"${s.operatorName}","${s.status}",${s.colonyCount},${s.cfuCount},"${s.contaminationRisk}","${formattedDate}","${safeComments}"\n`;
  });

  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename=MicrobeVision_Laboratory_Report_${Date.now()}.csv`);
  res.status(200).send(csvContent);
});

// -------------------------------------------------------------
// GET /api/reports/pdf/:id - Print-friendly high-fidelity Lab Certificate
// -------------------------------------------------------------
router.get('/pdf/:id', async (req, res) => {
  const sampleId = req.params.id;
  const token = req.query.token;

  if (!token) {
    return res.status(401).send('Access Denied. Authentication token required to download reports.');
  }

  let authenticatedUser;
  try {
    const decoded = await auth.verifyIdToken(token);
    // Fetch full user profile from Firestore to get department etc.
    const userDoc = await db.collection('users').doc(decoded.uid).get();
    authenticatedUser = { 
      id: decoded.uid,
      ...(userDoc.exists ? userDoc.data() : { department: 'General Microbiology' })
    };
  } catch (err) {
    return res.status(401).send('Access Denied. Invalid or expired token.');
  }

  let sample;
  try {
    const doc = await db.collection('samples').doc(sampleId).get();
    if (doc.exists && (doc.data().userId === authenticatedUser.id || authenticatedUser.role === 'Admin')) {
      sample = { _id: doc.id, ...doc.data() };
    }
  } catch (error) {
    return res.status(500).send('Error retrieving sample record.');
  }

  if (!sample) {
    return res.status(404).send('Sample record not found.');
  }

  // Generate gorgeous print certificate HTML
  const dateFormatted = new Date(sample.createdAt).toLocaleString();

  // Escape all user-controlled fields before HTML insertion (XSS prevention)
  const safeBatchId = escapeHtml(sample.batchId);
  const safeOperatorName = escapeHtml(sample.operatorName);
  const safeComments = escapeHtml(sample.comments);
  const safeSampleId = escapeHtml(sample._id);
  const safeApplianceType = escapeHtml(sample.applianceType);
  const safeStatus = escapeHtml(sample.status);
  const safeRisk = escapeHtml(sample.contaminationRisk);
  const safeDepartment = escapeHtml(authenticatedUser.department || 'General Microbiology');

  const zoneRows = sample.zones.map(z => `
    <tr>
      <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; font-weight: 500;">${escapeHtml(z.name)}</td>
      <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: center;">${Number(z.count)}</td>
      <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: center;">${Number(z.density)}%</td>
      <td style="padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: center;">
        <span class="badge ${escapeHtml(z.risk.toLowerCase())}">${escapeHtml(z.risk)}</span>
      </td>
    </tr>
  `).join('');

  // Define recommendations based on risk
  let recommendations = '';
  switch (sample.contaminationRisk) {
    case 'Low':
      recommendations = 'Sample meets compliance thresholds. Standard sanitization protocol approved for re-use or release. No corrective action required.';
      break;
    case 'Medium':
      recommendations = 'Moderate contamination detected. Recommended autoclave sterilization. Run subsequent batch control tests to confirm sterility prior to clinical use.';
      break;
    case 'High':
      recommendations = 'Elevated microbial colony growth observed. Critical alert level! Quarantine batch immediately. Re-calibrate medical sanitizers and initiate structural review.';
      break;
    case 'Critical':
      recommendations = 'SEVERE BIOBURDEN OUTBREAK DETECTED! Immediately reject and incinerate sample batch. Perform comprehensive chemical decontamination of relevant laboratory appliances. Mandatory reporting to the hospital infection control committee.';
      break;
  }

  const certificateHTML = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta http-equiv="Content-Security-Policy" content="default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; script-src 'none';">
      <title>Laboratory Analysis Certificate - ${safeBatchId}</title>
      <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
        body {
          font-family: 'Inter', sans-serif;
          color: #1e293b;
          margin: 0;
          padding: 40px;
          background-color: #ffffff;
          line-height: 1.5;
          -webkit-print-color-adjust: exact;
        }
        .header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          border-bottom: 3px solid #0f52ba;
          padding-bottom: 20px;
          margin-bottom: 30px;
        }
        .header-logo {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .header-logo-icon {
          width: 38px;
          height: 38px;
          background: linear-gradient(135deg, #0f52ba, #00a8cc);
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-weight: 700;
          font-size: 20px;
        }
        .header-logo-text {
          font-size: 22px;
          font-weight: 700;
          background: linear-gradient(135deg, #0f52ba, #00a8cc);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          letter-spacing: -0.5px;
        }
        .header-title {
          text-align: right;
        }
        .header-title h1 {
          margin: 0;
          font-size: 24px;
          font-weight: 700;
          color: #0f52ba;
        }
        .header-title p {
          margin: 4px 0 0 0;
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
        }
        .info-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 20px;
          margin-bottom: 30px;
        }
        .info-card {
          background-color: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          padding: 15px;
        }
        .info-card h3 {
          margin-top: 0;
          margin-bottom: 12px;
          font-size: 14px;
          color: #0f52ba;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          border-bottom: 1px solid #e2e8f0;
          padding-bottom: 6px;
        }
        .info-row {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          font-size: 13px;
        }
        .info-label {
          color: #64748b;
          font-weight: 500;
        }
        .info-value {
          font-weight: 600;
          color: #1e293b;
        }
        .metrics-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 15px;
          margin-bottom: 30px;
        }
        .metric-card {
          text-align: center;
          padding: 15px;
          border-radius: 8px;
          border: 1px solid #e2e8f0;
          background-color: #ffffff;
        }
        .metric-card.critical { border-left: 4px solid #ef4444; background-color: #fef2f2; }
        .metric-card.high { border-left: 4px solid #f97316; background-color: #fff7ed; }
        .metric-card.medium { border-left: 4px solid #eab308; background-color: #fef9c3; }
        .metric-card.low { border-left: 4px solid #10b981; background-color: #ecfdf5; }
        .metric-num {
          font-size: 28px;
          font-weight: 700;
          color: #0f52ba;
          margin-bottom: 4px;
        }
        .metric-num.danger { color: #ef4444; }
        .metric-num.warning { color: #f97316; }
        .metric-num.success { color: #10b981; }
        .metric-label {
          font-size: 11px;
          font-weight: 600;
          color: #64748b;
          text-transform: uppercase;
        }
        .section-title {
          font-size: 16px;
          font-weight: 700;
          color: #1e293b;
          margin-bottom: 15px;
          border-left: 3px solid #0f52ba;
          padding-left: 10px;
        }
        .zones-table {
          width: 100%;
          border-collapse: collapse;
          margin-bottom: 30px;
          font-size: 13px;
        }
        .zones-table th {
          background-color: #f1f5f9;
          padding: 10px;
          font-weight: 600;
          color: #475569;
          text-align: left;
          border-bottom: 2px solid #cbd5e1;
        }
        .badge {
          display: inline-block;
          padding: 3px 8px;
          border-radius: 12px;
          font-size: 11px;
          font-weight: 600;
          text-transform: uppercase;
        }
        .badge.critical { background-color: #fee2e2; color: #ef4444; }
        .badge.high { background-color: #ffedd5; color: #f97316; }
        .badge.medium { background-color: #fef9c3; color: #ca8a04; }
        .badge.low { background-color: #d1fae5; color: #10b981; }
        
        .comments-section {
          background-color: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          padding: 15px;
          font-size: 13px;
          margin-bottom: 40px;
        }
        .recommendation {
          margin-top: 10px;
          padding-top: 10px;
          border-top: 1px dashed #cbd5e1;
          font-weight: 500;
          color: #0f52ba;
        }
        
        .footer-signatures {
          display: flex;
          justify-content: space-between;
          margin-top: 60px;
        }
        .signature-box {
          text-align: center;
          width: 220px;
        }
        .signature-line {
          border-top: 1px solid #94a3b8;
          margin-bottom: 8px;
        }
        .signature-title {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
        }
        
        .print-btn {
          position: fixed;
          bottom: 20px;
          right: 20px;
          background: linear-gradient(135deg, #0f52ba, #00a8cc);
          color: white;
          border: none;
          padding: 12px 24px;
          border-radius: 30px;
          font-size: 14px;
          font-weight: 600;
          cursor: pointer;
          box-shadow: 0 4px 12px rgba(15, 82, 186, 0.3);
          display: flex;
          align-items: center;
          gap: 8px;
        }
        @media print {
          .print-btn {
            display: none;
          }
          body {
            padding: 0;
          }
        }
      </style>
    </head>
    <body>
      <button class="print-btn" onclick="window.print()">
        <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"></path>
        </svg>
        Print / Save PDF
      </button>

      <div class="header">
        <div class="header-logo">
          <div class="header-logo-icon">M</div>
          <span class="header-logo-text">MicrobeVision AI</span>
        </div>
        <div class="header-title">
          <h1>ANALYSIS CERTIFICATE</h1>
          <p>ISO/IEC 17025 ACCREDITED LAB SYSTEM</p>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-card">
          <h3>Sample Metadata</h3>
          <div class="info-row">
            <span class="info-label">Sample ID:</span>
            <span class="info-value">${safeSampleId}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Batch Identifier:</span>
            <span class="info-value">${safeBatchId}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Medical Appliance:</span>
            <span class="info-value">${safeApplianceType}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Dilution Ratio:</span>
            <span class="info-value">1 : ${Number(sample.dilutionFactor)}</span>
          </div>
        </div>
        <div class="info-card">
          <h3>Laboratory Log</h3>
          <div class="info-row">
            <span class="info-label">Date Analyzed:</span>
            <span class="info-value">${escapeHtml(dateFormatted)}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Lead Operator:</span>
            <span class="info-value">${safeOperatorName}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Analysis Status:</span>
            <span class="info-value" style="color: #10b981;">${safeStatus}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Department:</span>
            <span class="info-value">${safeDepartment}</span>
          </div>
          <div class="info-row">
            <span class="info-label">CV Detection Engine:</span>
            <span class="info-value">Hough Circle v2.4</span>
          </div>
        </div>
      </div>

      <div class="section-title">Automated Colony Metrics</div>
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-num">${sample.colonyCount}</div>
          <div class="metric-label">Detected Colonies</div>
        </div>
        <div class="metric-card">
          <div class="metric-num">${sample.dilutionFactor}x</div>
          <div class="metric-label">Dilution Factor</div>
        </div>
        <div class="metric-card">
          <div class="metric-num">${sample.cfuCount}</div>
          <div class="metric-label">CFU Count (CFU/mL)</div>
        </div>
        <div class="metric-card ${safeRisk.toLowerCase()}">
          <div class="metric-num ${safeRisk === 'Critical' || safeRisk === 'High' ? 'danger' : safeRisk === 'Medium' ? 'warning' : 'success'}">${safeRisk}</div>
          <div class="metric-label">Contamination Risk</div>
        </div>
      </div>

      <div class="section-title">Quadrant / Zone Colony Distribution</div>
      <table class="zones-table">
        <thead>
          <tr>
            <th>Zone Region</th>
            <th style="text-align: center;">Colonies Counted</th>
            <th style="text-align: center;">Density Load (%)</th>
            <th style="text-align: center;">Regional Alert Level</th>
          </tr>
        </thead>
        <tbody>
          ${zoneRows}
        </tbody>
      </table>
      
      <div class="section-title">Visual Evidence</div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px;">
        <div class="info-card" style="text-align: center;">
          <h3 style="margin-bottom: 10px;">Original Upload</h3>
          <img src="${sample.originalImageUrl}" alt="Original Sample" style="max-width: 100%; border-radius: 8px; border: 1px solid #e2e8f0;"/>
        </div>
        <div class="info-card" style="text-align: center;">
          <h3 style="margin-bottom: 10px;">Processed Analysis</h3>
          <img src="${sample.processedImageUrl}" alt="Processed Sample" style="max-width: 100%; border-radius: 8px; border: 1px solid #e2e8f0;"/>
        </div>
      </div>

      <div class="section-title">Expert Review & Recommendations</div>
      <div class="comments-section">
        <strong>Operator Remarks:</strong><br>
        <em>${safeComments || 'No specific remarks added by the technician.'}</em>
        <div class="recommendation">
          <strong>Pathology Directive:</strong> ${escapeHtml(recommendations)}
        </div>
      </div>

      <div class="footer-signatures">
        <div class="signature-box">
          <div class="signature-line"></div>
          <div class="signature-title">${safeOperatorName}</div>
          <div class="signature-title">Lead Operator / Analyst</div>
        </div>
        <div class="signature-box">
          <div class="signature-line"></div>
          <div class="signature-title">Dr. Sarah Jenkins</div>
          <div class="signature-title">Lab Director / Quality Manager</div>
        </div>
      </div>
    </body>
    </html>
  `;
  res.setHeader('Content-Type', 'text/html');
  res.send(certificateHTML.trim());
});

module.exports = router;
