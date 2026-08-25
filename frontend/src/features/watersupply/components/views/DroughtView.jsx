export function DroughtView({ notify, userName, initials }) {
  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Drought alert system</h1>
          <p>Risk level based on tank level, season and operational demand.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Current risk gauge</h2>
            <span className="ws-tag orange">Alert</span>
          </div>
          <div className="ws-panel-body">
            <div className="ws-risk-gauge">
              <div className="ws-risk-step">Normal</div>
              <div className="ws-risk-step">Vigilance</div>
              <div className="ws-risk-step active">Alert</div>
              <div className="ws-risk-step">Critical</div>
            </div>
            <div className="ws-timeline">
              <span style={{ height: '32%' }}></span>
              <span style={{ height: '38%' }}></span>
              <span className="warn" style={{ height: '62%' }}></span>
              <span className="warn" style={{ height: '72%' }}></span>
              <span className="danger" style={{ height: '86%' }}></span>
            </div>
          </div>
        </div>

        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Recommendations</h2></div>
            <div className="ws-panel-body">
              <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                Reduce non-essential washing by 25%, keep irrigation on humidity-triggered mode, and reserve pumped water for critical crop blocks.
              </p>
            </div>
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Critical automation</h2></div>
            <div className="ws-panel-body">
              <span className="ws-tag red">If critical</span>
              <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                Propose automatic postponement for Zone E and Zone F irrigations until reservoir recovers above 25%.
              </p>
              <button className="ws-chip active" onClick={() => notify('Postponement plan prepared.')}>Prepare postponement plan</button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}