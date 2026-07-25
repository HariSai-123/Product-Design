import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Chart, CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler } from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';
import { apiExportCSV } from '../../services/api';

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler);

function animateValue(setter, target, duration = 1000) {
  let start = null;
  function step(ts) {
    if (!start) start = ts;
    const progress = Math.min((ts - start) / duration, 1);
    setter(Math.floor(progress * target));
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

export default function DashboardPanel({ samples, setActiveSample, showNotification }) {
  const navigate = useNavigate();
  const isDark = document.body.classList.contains('dark-theme');
  const textColor = isDark ? '#94a3b8' : '#64748b';
  const borderColor = isDark ? '#1e293b' : '#e2e8f0';

  if (samples.length === 0) {
    return (
      <div id="dashboard-panel" className="panel-view active">
        <div className="panel-header"><h2>Microbiological Quality Dashboard</h2><p>Real-time statistics of analyzed medical appliance agar samples.</p></div>
        <div className="glass-card" style={{ padding: 40, textAlign: 'center', marginBottom: 30, borderLeft: '5px solid var(--accent)' }}>
          <h2 style={{ fontSize: 26, marginBottom: 12, color: 'var(--primary)' }}>Welcome to MicrobeVision AI Diagnostics</h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: 620, margin: '0 auto 30px auto', fontSize: 14 }}>
            Get started by uploading your first agar plate sample scan.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/app/upload')}>Upload First Sample</button>
        </div>
      </div>
    );
  }

  let totalColonies = 0, totalCFU = 0, healthyCount = 0, dangerCount = 0;
  samples.forEach(s => {
    totalColonies += s.colonyCount;
    totalCFU += s.cfuCount;
    if (s.contaminationRisk === 'Low') healthyCount++;
    if (s.contaminationRisk === 'High' || s.contaminationRisk === 'Critical') dangerCount++;
  });
  const avgCFU = Math.round(totalCFU / samples.length);
  const contaminationRate = ((dangerCount / samples.length) * 100).toFixed(1);

  const historyReversed = [...samples].reverse();
  const lineData = {
    labels: historyReversed.map(s => s.batchId),
    datasets: [
      { label: 'Colonies Counted', data: historyReversed.map(s => s.colonyCount), borderColor: '#0f52ba', backgroundColor: 'rgba(15,82,186,0.1)', fill: true, tension: 0.3 },
      { label: 'Calculated CFU/mL', data: historyReversed.map(s => s.cfuCount), borderColor: '#00a8cc', backgroundColor: 'transparent', borderDash: [5, 5], tension: 0.3 },
    ],
  };
  const riskCounts = { Low: 0, Medium: 0, High: 0, Critical: 0 };
  samples.forEach(s => riskCounts[s.contaminationRisk]++);
  const doughnutData = {
    labels: ['Low', 'Medium', 'High', 'Critical'],
    datasets: [{ data: [riskCounts.Low, riskCounts.Medium, riskCounts.High, riskCounts.Critical], backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#7f1d1d'], borderWidth: 0 }],
  };
  const chartOptions = { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: textColor } } }, scales: { x: { grid: { color: borderColor }, ticks: { color: textColor } }, y: { grid: { color: borderColor }, ticks: { color: textColor } } } };

  return (
    <div id="dashboard-panel" className="panel-view active">
      <div className="panel-header"><h2>Microbiological Quality Dashboard</h2><p>Real-time statistics of analyzed medical appliance agar samples.</p></div>
      <div className="stats-row">
        {[
          { title: 'Total Samples', val: samples.length, sub: 'QA Standard Approved' },
          { title: 'Colonies Detected', val: totalColonies, sub: 'Cumulative Bioburden' },
          { title: 'Avg CFU Count', val: avgCFU, sub: 'Normal Distribution' },
          { title: 'Contamination Rate', val: `${contaminationRate}%`, sub: 'High/Critical Alerts', danger: true },
          { title: 'Healthy Samples', val: healthyCount, sub: 'Zero Contamination', success: true },
        ].map(c => (
          <div key={c.title} className="glass-card stat-card">
            <span className="stat-card-title">{c.title}</span>
            <div className="stat-card-val" style={{ color: c.danger ? 'var(--color-danger)' : c.success ? 'var(--color-success)' : undefined }}>{c.val}</div>
            <span className="stat-card-trend">{c.sub}</span>
          </div>
        ))}
      </div>
      <div className="charts-grid">
        <div className="glass-card"><h3 style={{ marginBottom: 20 }}>Colony Accrual Trends</h3><div className="chart-container"><Line data={lineData} options={chartOptions} /></div></div>
        <div className="glass-card"><h3 style={{ marginBottom: 20 }}>Contamination Risk Breakdown</h3><div className="chart-container"><Doughnut data={doughnutData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { color: textColor } } } }} /></div></div>
      </div>
      <div className="glass-card">
        <h3 style={{ marginBottom: 16 }}>Recent Quality Control Scans</h3>
        <div className="history-table-wrapper">
          <table className="history-table">
            <thead><tr><th>Batch ID</th><th>Appliance Type</th><th style={{ textAlign: 'center' }}>Colonies</th><th style={{ textAlign: 'center' }}>CFU Count (CFU/mL)</th><th>Alert Status</th><th style={{ textAlign: 'right' }}>Action</th></tr></thead>
            <tbody>
              {samples.slice(0, 5).map(s => (
                <tr key={s.id || s._id}>
                  <td style={{ padding: 12, fontWeight: 700 }}>{s.batchId}</td>
                  <td style={{ padding: 12, color: 'var(--text-secondary)' }}>{s.applianceType}</td>
                  <td style={{ padding: 12, textAlign: 'center' }}>{s.colonyCount}</td>
                  <td style={{ padding: 12, textAlign: 'center' }}>{s.cfuCount}</td>
                  <td style={{ padding: 12 }}><span className={`badge badge-${s.contaminationRisk.toLowerCase()}`}>{s.contaminationRisk}</span></td>
                  <td style={{ padding: 12, textAlign: 'right' }}>
                    <button className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: 12 }} onClick={() => { setActiveSample(s); navigate('/app/analysis'); }}>View</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
