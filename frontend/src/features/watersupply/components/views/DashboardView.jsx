export function DashboardView({ onNavigate, notify, userName, initials }) {
  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water supply dashboard</h1>
          <p>Operational view for irrigation, tanks, quality and drought risk.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-kpi-grid">
        <div className="ws-kpi-card">
          <div className="ws-kpi-label">Reservoir level</div>
          <div className="ws-kpi-value">58%</div>
        </div>
        <div className="ws-kpi-card">
          <div className="ws-kpi-label">Rain tank</div>
          <div className="ws-kpi-value">72%</div>
        </div>
        <div className="ws-kpi-card orange">
          <div className="ws-kpi-label">Active alerts</div>
          <div className="ws-kpi-value">3</div>
        </div>
        <div className="ws-kpi-card hero">
          <div className="ws-kpi-label">Water saved this month</div>
          <div className="ws-kpi-value">18%</div>
        </div>
      </div>

      <div className="ws-dashboard-strip">
        <div className="ws-status-card">
          <span className="ws-tag green">Irrigation</span>
          <strong>2 950 L</strong>
          <span>Planned today across 5 active zones. Zone D is postponed automatically.</span>
        </div>
        <div className="ws-status-card">
          <span className="ws-tag orange">Drought</span>
          <strong>Alert</strong>
          <span>Demand is high and reservoir recovery is slower than expected.</span>
        </div>
        <div className="ws-status-card">
          <span className="ws-tag red">Quality</span>
          <strong>1 issue</strong>
          <span>Reservoir pH is out of range and needs retesting before processing use.</span>
        </div>
      </div>

      <div className="ws-dashboard-grid">
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Today's field operations</h2>
              <span>Updated 5 min ago</span>
            </div>
            <div className="ws-panel-body ws-summary-list">
              <div className="ws-summary-item">
                <span className="ws-summary-dot green"></span>
                <div className="ws-summary-title">Zone A completed<small>05:30 - 06:15, 1 200 L used on drip network</small></div>
                <span className="ws-tag green">Done</span>
              </div>
              <div className="ws-summary-item">
                <span className="ws-summary-dot orange"></span>
                <div className="ws-summary-title">Zone B running<small>Flow at 15.5 L/min, lower than the 20 L/min reference</small></div>
                <span className="ws-tag orange">Watch</span>
              </div>
              <div className="ws-summary-item">
                <span className="ws-summary-dot"></span>
                <div className="ws-summary-title">Zone C planned<small>17:00 - 17:30, nursery sprinkler, 400 L planned</small></div>
                <span className="ws-tag primary">Planned</span>
              </div>
              <div className="ws-summary-item">
                <span className="ws-summary-dot red"></span>
                <div className="ws-summary-title">Zone D postponed<small>Soil humidity is above seasonal threshold, next check tomorrow</small></div>
                <span className="ws-tag red">Saved</span>
              </div>
            </div>
          </div>

          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Consumption snapshot</h2>
              <span>Actual vs reference</span>
            </div>
            <div className="ws-panel-body">
              <div className="ws-chart">
                <div className="ws-bar-pair"><div className="ws-bar ref" style={{ height: '64%' }}></div><div className="ws-bar" style={{ height: '51%' }}></div><span>Irr.</span></div>
                <div className="ws-bar-pair"><div className="ws-bar ref" style={{ height: '34%' }}></div><div className="ws-bar" style={{ height: '39%' }}></div><span>Wash</span></div>
                <div className="ws-bar-pair"><div className="ws-bar ref" style={{ height: '52%' }}></div><div className="ws-bar" style={{ height: '46%' }}></div><span>Proc.</span></div>
                <div className="ws-bar-pair"><div className="ws-bar ref" style={{ height: '28%' }}></div><div className="ws-bar" style={{ height: '29%' }}></div><span>Clean</span></div>
              </div>
            </div>
          </div>
        </div>

        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Water sources</h2>
              <button className="ws-chip active" onClick={() => onNavigate('rainwater')}>Rain tank details</button>
            </div>
            <div className="ws-panel-body">
              <table className="ws-table">
                <tbody>
                  <tr><td>Main reservoir</td><td><strong>58%</strong></td><td><span className="ws-tag orange">Alert</span></td></tr>
                  <tr><td>Rain tank</td><td><strong>72%</strong></td><td><span className="ws-tag green">Usable</span></td></tr>
                  <tr><td>Borehole</td><td><strong>Online</strong></td><td><span className="ws-tag primary">Reserve</span></td></tr>
                </tbody>
              </table>
            </div>
          </div>

          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Immediate actions</h2></div>
            <div className="ws-panel-body">
              <div className="ws-quick-actions">
                <button className="ws-action-btn" onClick={() => onNavigate('irrigation')}>Manage irrigation</button>
                <button className="ws-action-btn secondary" onClick={() => onNavigate('consumption')}>Review consumption</button>
                <button className="ws-action-btn warning" onClick={() => notify('Rainwater priority activated for washing and processing.')}>Use rainwater first</button>
                <button className="ws-action-btn secondary" onClick={() => onNavigate('quality')}>Record quality test</button>
                <button className="ws-action-btn warning" onClick={() => onNavigate('drought')}>Prepare drought plan</button>
              </div>
            </div>
          </div>

          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Current recommendations</h2></div>
            <div className="ws-panel-body">
              <span className="ws-tag orange">Water saving</span>
              <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                Reduce non-essential washing by 25%, keep humidity-triggered irrigation active, and reserve pumped water for crop blocks A, B and C.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}