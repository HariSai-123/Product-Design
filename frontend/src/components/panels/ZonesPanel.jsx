import { useNavigate } from 'react-router-dom';

const ZONE_NAMES = ['Zone A (North-West)', 'Zone B (North-East)', 'Zone C (South-West)', 'Zone D (South-East)'];

export default function ZonesPanel({ activeSample }) {
  const navigate = useNavigate();

  if (!activeSample || !activeSample.zones || activeSample.zones.length === 0) {
    return (
      <div id="zones-panel" className="panel-view active">
        <div className="panel-header"><h2>Zone-wise Colony Distribution Analysis</h2><p>Segment circular petri scans into quadrant zones.</p></div>
        <div className="glass-card" style={{ padding: 40, textAlign: 'center' }}>
          <p style={{ color: 'var(--text-secondary)' }}>No sample loaded. Upload a sample first to see zone analysis.</p>
          <button className="btn btn-primary" style={{ marginTop: 20 }} onClick={() => navigate('/app/upload')}>Go to Upload</button>
        </div>
      </div>
    );
  }

  const zones = activeSample.zones;
  const maxCount = Math.max(...zones.map(z => z.count));

  const QUAD_POSITIONS = [
    { top: 0, left: 0 },
    { top: 0, right: 0 },
    { bottom: 0, left: 0 },
    { bottom: 0, right: 0 },
  ];

  return (
    <div id="zones-panel" className="panel-view active">
      <div className="panel-header"><h2>Zone-wise Colony Distribution Analysis</h2><p>Segment circular petri scans into quadrant zones to analyze spatial contamination hot-zones.</p></div>
      <div className="glass-card zone-grid-wrapper">
        <div style={{ textAlign: 'center' }}>
          <h3 style={{ marginBottom: 20, color: 'var(--primary)' }}>Agar Quad Hotspot Grid</h3>
          <div className="zone-layout-preview">
            <div id="zone-layout-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gridTemplateRows: '1fr 1fr', width: 220, height: 220, position: 'relative', border: '2px solid var(--border-color)', borderRadius: '50%', overflow: 'hidden', margin: '0 auto' }}>
              {zones.map((zone, idx) => {
                const isHotspot = zone.count === maxCount && maxCount > 0;
                return (
                  <div key={idx} style={{
                    background: isHotspot ? 'rgba(239,68,68,0.2)' : 'var(--bg-primary)',
                    border: `1px ${isHotspot ? 'solid rgba(239,68,68,0.4)' : 'dashed var(--border-color)'}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexDirection: 'column', fontSize: 11, fontWeight: 700, color: isHotspot ? '#ef4444' : 'var(--text-secondary)'
                  }}>
                    <div style={{ fontSize: 18, fontWeight: 800 }}>{zone.count}</div>
                    <div style={{ fontSize: 9 }}>{['NW', 'NE', 'SW', 'SE'][idx]}</div>
                  </div>
                );
              })}
            </div>
          </div>
          <div className="canvas-legend" style={{ justifyContent: 'center', marginTop: 20 }}>
            <div className="legend-item"><span className="legend-dot auto" style={{ background: 'rgba(239,68,68,0.3)' }}></span><span>Hotspot Alert Quad</span></div>
            <div className="legend-item"><span className="legend-dot manual" style={{ background: 'var(--bg-secondary)', border: '1px dashed var(--text-muted)' }}></span><span>Standard Quads</span></div>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 12 }}>
          {zones.map((zone, idx) => {
            const isHotspot = zone.count === maxCount && maxCount > 0;
            return (
              <div key={idx} className="glass-card" style={{ padding: '16px 20px', borderLeft: `4px solid ${isHotspot ? '#ef4444' : 'var(--border-color)'}`, background: isHotspot ? 'rgba(239,68,68,0.05)' : 'var(--bg-primary)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <strong style={{ fontSize: 13 }}>{zone.name || ZONE_NAMES[idx]}</strong>
                  <span className={`badge badge-${zone.risk?.toLowerCase() || 'low'}`}>{zone.risk}</span>
                </div>
                <div style={{ display: 'flex', gap: 20, fontSize: 12, color: 'var(--text-secondary)' }}>
                  <span>Colonies: <strong style={{ color: 'var(--text-primary)' }}>{zone.count}</strong></span>
                  <span>Density: <strong style={{ color: 'var(--text-primary)' }}>{zone.density}%</strong></span>
                </div>
                <div style={{ marginTop: 8, height: 6, borderRadius: 3, background: 'var(--border-color)', overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${zone.density}%`, background: isHotspot ? '#ef4444' : 'var(--primary)', borderRadius: 3, transition: 'width 0.5s ease' }} />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
