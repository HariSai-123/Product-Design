import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiUpdateDetections } from '../../services/api';

function getRiskLevel(count) {
  if (count <= 5) return 'Low';
  if (count <= 25) return 'Medium';
  if (count <= 60) return 'High';
  return 'Critical';
}

export default function ColonyCanvasPanel({ activeSample, setActiveSample, showNotification, reloadSamples }) {
  const canvasRef = useRef(null);
  const imageRef = useRef(new Image());
  const [circles, setCircles] = useState([]);
  const [confidence, setConfidence] = useState(0.7);
  const [heatmap, setHeatmap] = useState(false);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!activeSample) return;
    const sample = activeSample;
    const imageUrl = sample.originalImageUrl || sample.originalImage;
    if (imageUrl) {
      // originalImageUrl is already stored as "/uploads/filename" in Firestore.
      // Only prepend /uploads/ if the path is a bare filename (no leading slash or http).
      let resolvedUrl;
      if (imageUrl.startsWith('http')) {
        resolvedUrl = imageUrl;
      } else if (imageUrl.startsWith('/')) {
        resolvedUrl = imageUrl; // already an absolute path like /uploads/xxx
      } else {
        resolvedUrl = `/uploads/${imageUrl}`;
      }
      imageRef.current.src = resolvedUrl;
      imageRef.current.onload = () => drawCanvas(sample.detections || []);
    } else {
      drawCanvas(sample.detections || []);
    }
    setCircles(sample.detections || []);
  }, [activeSample]);

  useEffect(() => {
    drawCanvas(circles);
  }, [circles, confidence, heatmap]);

  const drawCanvas = (dets) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw background image if loaded
    if (imageRef.current.complete && imageRef.current.naturalHeight !== 0) {
      ctx.drawImage(imageRef.current, 0, 0, canvas.width, canvas.height);
    } else {
      ctx.fillStyle = '#060b13';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.strokeStyle = '#0f52ba';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(200, 200, 185, 0, Math.PI * 2);
      ctx.stroke();
    }

    if (heatmap) {
      const offscreen = document.createElement('canvas');
      offscreen.width = canvas.width;
      offscreen.height = canvas.height;
      const octx = offscreen.getContext('2d');
      dets.forEach(d => {
        if ((d.confidence || 1) < confidence) return;
        const g = octx.createRadialGradient(d.x, d.y, 0, d.x, d.y, 30);
        g.addColorStop(0, 'rgba(239,68,68,0.4)');
        g.addColorStop(1, 'rgba(239,68,68,0)');
        octx.fillStyle = g;
        octx.beginPath();
        octx.arc(d.x, d.y, 30, 0, Math.PI * 2);
        octx.fill();
      });
      ctx.drawImage(offscreen, 0, 0);
    }

    dets.forEach(d => {
      if ((d.confidence || 1) < confidence) return;
      const isManual = d.manual;
      ctx.strokeStyle = isManual ? '#f97316' : 'rgba(0,168,204,0.85)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(d.x, d.y, d.radius || 8, 0, Math.PI * 2);
      ctx.stroke();
      ctx.fillStyle = isManual ? '#f97316' : '#00a8cc';
      ctx.beginPath();
      ctx.arc(d.x, d.y, 2.5, 0, Math.PI * 2);
      ctx.fill();
    });
  };

  const handleCanvasClick = (e) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = (e.clientX - rect.left) * scaleX;
    const y = (e.clientY - rect.top) * scaleY;

    if (e.altKey || e.button === 2) {
      // Delete nearest colony
      const threshold = 15;
      setCircles(prev => {
        const nearest = prev.reduce((best, c, i) => {
          const dist = Math.hypot(c.x - x, c.y - y);
          return dist < best.dist ? { i, dist } : best;
        }, { i: -1, dist: threshold });
        if (nearest.i >= 0) return prev.filter((_, i) => i !== nearest.i);
        return prev;
      });
    } else {
      // Add new colony
      setCircles(prev => [...prev, { x: Math.round(x), y: Math.round(y), radius: 9, confidence: 1, manual: true }]);
    }
  };

  const handleSave = async () => {
    if (!activeSample) { showNotification('danger', 'No active sample to save.'); return; }
    setSaving(true);
    try {
      const sampleId = activeSample.id || activeSample._id;
      const data = await apiUpdateDetections(sampleId, circles);
      setActiveSample(data.sample);
      await reloadSamples();
      showNotification('success', 'Colony annotations saved successfully.');
    } catch (err) {
      showNotification('danger', err.message);
    } finally {
      setSaving(false);
    }
  };

  const visibleCount = circles.filter(c => (c.confidence || 1) >= confidence).length;
  const risk = getRiskLevel(visibleCount);
  const cfu = activeSample ? visibleCount * activeSample.dilutionFactor : visibleCount;

  return (
    <div id="analysis-panel" className="panel-view active">
      <div className="panel-header"><h2>Colony Canvas Engine</h2><p>Interactively verify, correct, and annotate AI-detected microbial blobs.</p></div>
      {!activeSample ? (
        <div className="glass-card" style={{ padding: 40, textAlign: 'center' }}>
          <p style={{ color: 'var(--text-secondary)' }}>No sample selected. Upload and analyze a sample first.</p>
          <button className="btn btn-primary" style={{ marginTop: 20 }} onClick={() => navigate('/app/upload')}>Go to Upload</button>
        </div>
      ) : (
        <div className="analysis-workspace">
          <div className="glass-card canvas-board-wrapper">
            <div className="canvas-board">
              <canvas
                ref={canvasRef}
                id="analysis-canvas-board"
                className="canvas-element"
                width={400}
                height={400}
                onClick={handleCanvasClick}
                onContextMenu={e => { e.preventDefault(); handleCanvasClick({ ...e, altKey: true }); }}
                style={{ cursor: 'crosshair' }}
              />
            </div>
            <div className="canvas-legend">
              <div className="legend-item"><span className="legend-dot auto"></span><span>AI Core Detection</span></div>
              <div className="legend-item"><span className="legend-dot manual"></span><span>Manual Annotation</span></div>
            </div>
            <div className="canvas-controls">
              <div style={{ flexGrow: 1 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)', display: 'flex', justifyContent: 'space-between' }}>
                  <span>Confidence Filter Threshold</span>
                  <span style={{ color: 'var(--accent)' }}>{Math.round(confidence * 100)}%</span>
                </label>
                <input type="range" min="0.5" max="0.95" step="0.05" value={confidence} onChange={e => setConfidence(parseFloat(e.target.value))} style={{ width: '100%', marginTop: 8 }} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 15, borderLeft: '1px solid var(--border-color)' }}>
                <input type="checkbox" id="checkbox-toggle-heatmap" style={{ width: 18, height: 18, cursor: 'pointer' }} checked={heatmap} onChange={e => setHeatmap(e.target.checked)} />
                <label htmlFor="checkbox-toggle-heatmap" style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)', cursor: 'pointer' }}>Density Heatmap</label>
              </div>
            </div>
            <p style={{ fontSize: 11, color: 'var(--text-secondary)', textAlign: 'center', width: '100%', marginTop: 8 }}>
              💡 <strong>Operator Tip:</strong> Click empty spaces to <strong>Add Colony</strong>. Alt-Click or Right-Click circles to <strong>Delete False Positive</strong>.
            </p>
          </div>

          <div className="analysis-results-meta">
            <div className="glass-card">
              <h3 style={{ marginBottom: 16, color: 'var(--primary)' }}>Active Diagnostic Metrics</h3>
              <div className="meta-summary-card">
                <div style={{ background: 'var(--bg-primary)', borderRadius: 8, padding: 15, border: '1px solid var(--border-color)' }}>
                  <span style={{ fontSize: 11, textTransform: 'uppercase', fontWeight: 700, color: 'var(--text-secondary)' }}>Counted Colonies</span>
                  <div style={{ fontSize: 32, fontWeight: 800, color: 'var(--primary)' }}>{visibleCount}</div>
                </div>
                <div style={{ background: 'var(--bg-primary)', borderRadius: 8, padding: 15, border: '1px solid var(--border-color)' }}>
                  <span style={{ fontSize: 11, textTransform: 'uppercase', fontWeight: 700, color: 'var(--text-secondary)' }}>Alert Risk Level</span>
                  <div style={{ marginTop: 8 }}><span className={`badge badge-${risk.toLowerCase()}`}>{risk}</span></div>
                </div>
              </div>
              <div style={{ background: 'var(--bg-primary)', borderRadius: 8, padding: 20, border: '1px solid var(--border-color)', marginTop: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span style={{ fontSize: 11, textTransform: 'uppercase', fontWeight: 700, color: 'var(--text-secondary)' }}>Calculated Bioburden (CFU/mL)</span>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2 }}>Includes plating dilution scalar</p>
                </div>
                <div style={{ fontSize: 36, fontWeight: 800, color: 'var(--accent)' }}>{cfu}</div>
              </div>
            </div>

            <div className="glass-card">
              <h3 style={{ marginBottom: 16, color: 'var(--primary)' }}>Sample Record Metadata</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, fontSize: 13 }}>
                {[
                  ['Batch ID', activeSample.batchId],
                  ['Appliance', activeSample.applianceType],
                  ['Operator', activeSample.operatorName],
                  ['Dilution Factor', activeSample.dilutionFactor],
                  ['Date Logged', new Date(activeSample.createdAt).toLocaleDateString()],
                ].map(([k, v]) => (
                  <React.Fragment key={k}>
                    <div><span style={{ color: 'var(--text-secondary)' }}>{k}:</span></div>
                    <div><strong>{String(v ?? '')}</strong></div>
                  </React.Fragment>
                ))}
              </div>
              <div style={{ marginTop: 24, display: 'flex', gap: 12 }}>
                <button className="btn btn-primary" style={{ flexGrow: 1 }} onClick={handleSave} disabled={saving}>
                  {saving ? 'Saving...' : 'Save Corrections'}
                </button>
                <button className="btn btn-secondary" onClick={() => navigate('/app/cfu')}>CFU Calculator ›</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
