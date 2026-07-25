import { useState } from 'react';

function getRisk(count) {
  if (count <= 5) return 'Low';
  if (count <= 25) return 'Medium';
  if (count <= 60) return 'High';
  return 'Critical';
}

export default function CFUPanel({ activeSample }) {
  const [colonies, setColonies] = useState(activeSample?.colonyCount || 0);
  const [dilution, setDilution] = useState(activeSample?.dilutionFactor || 1);
  const [volume, setVolume] = useState(0.1);
  const [result, setResult] = useState(null);

  const calculate = (e) => {
    e.preventDefault();
    const cfu = (parseInt(colonies) * parseInt(dilution)) / parseFloat(volume);
    setResult({ cfu: Math.round(cfu), risk: getRisk(parseInt(colonies)) });
  };

  return (
    <div id="cfu-panel" className="panel-view active">
      <div className="panel-header"><h2>CFU & Plating Density Calculator</h2><p>Perform standardized clinical dilutions calculations and record density statistics.</p></div>
      <div className="glass-card cfu-calc-box">
        <div>
          <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>Plating Metrics Input</h3>
          <form onSubmit={calculate}>
            <div className="form-group">
              <label htmlFor="cfu-raw-colonies">Raw Colony Count (n)</label>
              <input type="number" id="cfu-raw-colonies" className="form-input" min="0" value={colonies} onChange={e => setColonies(e.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="cfu-dilution">Plating Dilution Factor (d)</label>
              <input type="number" id="cfu-dilution" className="form-input" min="1" value={dilution} onChange={e => setDilution(e.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="cfu-volume">Volume Plated in mL (v)</label>
              <input type="number" id="cfu-volume" className="form-input" min="0.01" step="0.01" value={volume} onChange={e => setVolume(e.target.value)} />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 10 }}>Calculate Concentration</button>
          </form>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', borderLeft: '1px solid var(--border-color)', paddingLeft: 30 }}>
          <div>
            <h3 style={{ marginBottom: 12, color: 'var(--primary)' }}>Pathology Concentration Result</h3>
            <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Concentration of bacterial organisms per milliliter:</span>
            <div style={{ fontSize: 42, fontWeight: 800, color: 'var(--accent)', margin: '16px 0', fontFamily: 'var(--font-heading)' }}>
              <span>{result ? result.cfu.toLocaleString() : 0}</span> <span style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-secondary)' }}>CFU/mL</span>
            </div>
            <div style={{ marginBottom: 20 }}>
              <span style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>Calculated Risk Factor:</span>
              <span className={`badge badge-${result ? result.risk.toLowerCase() : 'low'}`}>{result ? result.risk : 'Low'}</span>
            </div>
          </div>
          <div>
            <strong style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Standardized Plating Formula:</strong>
            <div className="formula-display">CFU/mL = (n × d) / v</div>
          </div>
        </div>
      </div>
    </div>
  );
}
