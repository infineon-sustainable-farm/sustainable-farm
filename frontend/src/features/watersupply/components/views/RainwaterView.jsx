export function RainwaterView({ notify, userName, initials }) {
  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Rainwater harvesting monitoring</h1>
          <p>Prioritize collected rainwater before pumped water whenever possible.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Current rain tank level</h2>
            <span className="ws-tag orange">Near overflow</span>
          </div>
          <div className="ws-gauge-wrap">
            <div className="ws-tank">
              <div>
                <div className="value">72%</div>
                <div className="sub">14 400 L stored</div>
              </div>
            </div>
          </div>
        </div>

        <div className="ws-stack">
          <div className="ws-metric-pill">
            <div>
              <div className="label">Collected this month</div>
              <div className="value">8 900 L</div>
            </div>
          </div>
          <div className="ws-metric-pill">
            <div>
              <div className="label">Coverage of non-irrigation needs</div>
              <div className="value">64%</div>
            </div>
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Recommendation</h2></div>
            <div className="ws-panel-body">
              <span className="ws-tag orange">Action</span>
              <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                Prioritize rainwater for washing and processing today. Keep 15% buffer before the next rainfall window.
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="ws-panel" style={{ marginTop: 22 }}>
        <div className="ws-panel-header">
          <h2>Collection events</h2>
          <span>Last 7 days</span>
        </div>
        <div className="ws-panel-body">
          <table className="ws-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Rainfall</th>
                <th>Collected volume</th>
                <th>Impact</th>
              </tr>
            </thead>
            <tbody>
              <tr><td>Aug 08</td><td>18 mm</td><td>2 600 L</td><td><span className="ws-tag orange">Overflow risk</span></td></tr>
              <tr><td>Aug 06</td><td>11 mm</td><td>1 580 L</td><td><span className="ws-tag green">Stored</span></td></tr>
              <tr><td>Aug 03</td><td>7 mm</td><td>940 L</td><td><span className="ws-tag green">Stored</span></td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}