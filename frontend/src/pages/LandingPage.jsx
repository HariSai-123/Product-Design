import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';

export default function LandingPage() {
  const heroCanvasRef = useRef(null);
  const demoCanvasRef = useRef(null);
  const thresholdRef = useRef(null);
  const countLabelRef = useRef(null);
  const riskLabelRef = useRef(null);

  // Hero preview canvas animation
  useEffect(() => {
    const canvas = heroCanvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let angle = 0;
    let animId;

    function draw() {
      ctx.fillStyle = '#060b13';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.strokeStyle = '#00a8cc';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(200, 200, 180, 0, Math.PI * 2);
      ctx.stroke();
      ctx.strokeStyle = 'rgba(0, 168, 204, 0.15)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(200, 200);
      ctx.lineTo(200 + Math.cos(angle) * 180, 200 + Math.sin(angle) * 180);
      ctx.stroke();
      const positions = [
        { x: 120, y: 150, r: 8 }, { x: 280, y: 220, r: 12 }, { x: 220, y: 110, r: 9 },
        { x: 160, y: 270, r: 6 }, { x: 240, y: 280, r: 7 }, { x: 110, y: 230, r: 11 }
      ];
      positions.forEach((p, idx) => {
        ctx.strokeStyle = 'rgba(15, 82, 186, 0.8)';
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.stroke();
        ctx.fillStyle = 'rgba(15, 82, 186, 0.3)';
        ctx.beginPath();
        ctx.arc(p.x, p.y, 3, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#00a8cc';
        ctx.font = '8px monospace';
        ctx.fillText(`C${idx + 1}:${(0.9 + idx * 0.015).toFixed(3)}`, p.x - 14, p.y - 12);
      });
      angle += 0.005;
      animId = requestAnimationFrame(draw);
    }
    draw();
    return () => cancelAnimationFrame(animId);
  }, []);

  // Interactive demo canvas
  useEffect(() => {
    const canvas = demoCanvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const circles = [];
    for (let i = 0; i < 40; i++) {
      const a = Math.random() * Math.PI * 2;
      const d = Math.sqrt(Math.random()) * 160;
      circles.push({
        x: 200 + Math.cos(a) * d,
        y: 200 + Math.sin(a) * d,
        r: 5 + Math.random() * 8,
        confidence: parseFloat((0.6 + Math.random() * 0.38).toFixed(2)),
      });
    }

    function drawDemo() {
      const threshold = parseFloat(thresholdRef.current?.value || 0.7);
      ctx.fillStyle = '#0a101d';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.strokeStyle = '#0f52ba';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.arc(200, 200, 180, 0, Math.PI * 2);
      ctx.stroke();

      let visibleCount = 0;
      let q1 = 0, q2 = 0, q3 = 0, q4 = 0;
      circles.forEach(c => {
        if (c.confidence < threshold) return;
        visibleCount++;
        if (c.x < 200) { if (c.y < 200) q1++; else q3++; } else { if (c.y < 200) q2++; else q4++; }
        ctx.strokeStyle = 'rgba(0, 168, 204, 0.7)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.arc(c.x, c.y, c.r, 0, Math.PI * 2);
        ctx.stroke();
        ctx.fillStyle = '#00a8cc';
        ctx.beginPath();
        ctx.arc(c.x, c.y, 2, 0, Math.PI * 2);
        ctx.fill();
      });

      const quads = [{ c: q1, sx: 0, sy: 0 }, { c: q2, sx: 200, sy: 0 }, { c: q3, sx: 0, sy: 200 }, { c: q4, sx: 200, sy: 200 }];
      let maxIdx = 0, maxQ = -1;
      quads.forEach((q, idx) => { if (q.c > maxQ) { maxQ = q.c; maxIdx = idx; } });
      if (maxQ > 0) {
        ctx.fillStyle = 'rgba(239, 68, 68, 0.08)';
        ctx.fillRect(quads[maxIdx].sx, quads[maxIdx].sy, 200, 200);
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.2)';
        ctx.lineWidth = 1;
        ctx.strokeRect(quads[maxIdx].sx, quads[maxIdx].sy, 200, 200);
      }
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.06)';
      ctx.setLineDash([5, 5]);
      ctx.beginPath();
      ctx.moveTo(200, 20); ctx.lineTo(200, 380);
      ctx.moveTo(20, 200); ctx.lineTo(380, 200);
      ctx.stroke();
      ctx.setLineDash([]);

      if (countLabelRef.current) countLabelRef.current.textContent = visibleCount;
      let risk = 'Low';
      if (visibleCount > 5) risk = 'Medium';
      if (visibleCount > 15) risk = 'High';
      if (visibleCount > 28) risk = 'Critical';
      if (riskLabelRef.current) {
        riskLabelRef.current.className = `badge badge-${risk.toLowerCase()}`;
        riskLabelRef.current.textContent = risk;
      }
    }

    drawDemo();
    const slider = thresholdRef.current;
    if (slider) slider.addEventListener('input', drawDemo);
    return () => { if (slider) slider.removeEventListener('input', drawDemo); };
  }, []);

  return (
    <div id="landing-view" className="lp-view">
      <header className="lp-container">
        <nav className="lp-nav">
          <div className="logo">
            <div className="logo-icon">M</div>
            <span>MicrobeVision AI</span>
          </div>
          <div className="lp-nav-links">
            <a href="#features-section" className="lp-nav-link">Features</a>
            <a href="#demo-section" className="lp-nav-link">Interactive Demo</a>
            <Link to="/login" className="btn btn-secondary">Sign In</Link>
            <Link to="/signup" className="btn btn-primary">Start Analysis</Link>
          </div>
        </nav>
      </header>

      <main className="lp-container">
        <section className="hero">
          <div>
            <div className="hero-badge">
              <span className="legend-dot auto" style={{ animation: 'pulse-demo 1s infinite' }}></span>
              Clinical Computer Vision transform v2.4 Active
            </div>
            <h1>Computer Vision System for Automated Microbial Colony Counting</h1>
            <p className="hero-desc">
              An enterprise-grade laboratory analytics platform designed for hospitals, QA scientists, and medical laboratories to automatically count, analyze, and profile microbial bioburden on medical appliances.
            </p>
            <div className="hero-btns">
              <Link to="/signup" className="btn btn-primary">Create Laboratory Account</Link>
              <a href="#demo-section" className="btn btn-secondary">Test Live Demo</a>
            </div>
          </div>
          <div className="hero-preview">
            <div className="demo-canvas-wrapper" style={{ maxWidth: 440, border: '8px solid var(--border-color)', boxShadow: '0 30px 60px rgba(15, 82, 186, 0.2)' }}>
              <canvas ref={heroCanvasRef} className="demo-canvas" width={400} height={400} />
            </div>
          </div>
        </section>
      </main>

      <section id="features-section" className="features-section lp-container">
        <div className="section-header">
          <h2>Laboratory-Grade Automated Diagnostics</h2>
          <p>Harness the power of localized circle-finding computer vision algorithms to streamline quality assurance protocols and eliminate manual logging bottlenecks.</p>
        </div>
        <div className="features-grid">
          {[
            { title: 'Automated Detection', desc: 'Instantly map, target, and segment bacterial colonies on surgical appliance surfaces with 98.4% visual accuracy.' },
            { title: 'CFU Density Profiling', desc: 'Input mathematical dilution matrices to immediately generate Colony Forming Unit density indicators per surface centimeter.' },
            { title: 'Zone Contamination', desc: 'Divide appliance scans into four specific spatial quadrants to identify high-density localized bioburden risk hotzones.' },
          ].map(f => (
            <div key={f.title} className="glass-card feature-card">
              <div className="feature-icon-wrapper">
                <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 002 2h2a2 2 0 002-2z" /></svg>
              </div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="demo-section" className="interactive-demo lp-container">
        <div className="demo-showcase">
          <div>
            <div className="hero-badge" style={{ backgroundColor: 'var(--accent-light)', color: 'var(--accent)' }}>Interactive Demo</div>
            <h2 style={{ marginBottom: 16 }}>Test Automated Annotation</h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 24 }}>Experience the system UI. Hover, click, and inspect colony bounding targets in this live agar simulator canvas.</p>
            <div style={{ marginBottom: 20 }}>
              <strong style={{ fontSize: 14, display: 'block', marginBottom: 8 }}>Live Controls</strong>
              <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Minimum Confidence Threshold</label>
              <input ref={thresholdRef} type="range" min="0.5" max="0.95" step="0.05" defaultValue="0.7" style={{ width: '100%' }} />
            </div>
            <div style={{ backgroundColor: 'var(--bg-primary)', borderRadius: 8, padding: 15, border: '1px solid var(--border-color)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <div>
                <span style={{ fontSize: 11, textTransform: 'uppercase', fontWeight: 700, color: 'var(--text-secondary)' }}>Detected Colonies</span>
                <div ref={countLabelRef} style={{ fontSize: 24, fontWeight: 800, color: 'var(--primary)' }}>32</div>
              </div>
              <div>
                <span style={{ fontSize: 11, textTransform: 'uppercase', fontWeight: 700, color: 'var(--text-secondary)' }}>Risk Factor</span>
                <div ref={riskLabelRef} className="badge badge-high" style={{ marginTop: 6 }}>High</div>
              </div>
            </div>
          </div>
          <div className="canvas-board-wrapper">
            <div className="demo-canvas-wrapper">
              <canvas ref={demoCanvasRef} className="demo-canvas" width={400} height={400} />
            </div>
            <div className="canvas-legend">
              <div className="legend-item"><span className="legend-dot auto"></span><span>AI Detections</span></div>
              <div className="legend-item"><span className="legend-dot manual"></span><span>Hotspots</span></div>
            </div>
          </div>
        </div>
      </section>

      <footer className="lp-container" style={{ borderTop: '1px solid var(--border-color)', padding: '40px 0', textAlign: 'center', color: 'var(--text-secondary)', fontSize: 13 }}>
        <p>&copy; 2026 MicrobeVision AI Diagnostics Inc. Standardized clinical operations ISO 9001:2015 certified.</p>
      </footer>
    </div>
  );
}
