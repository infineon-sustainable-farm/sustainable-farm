export function DripView({ notify, userName, initials }) {
  const zones = [
    { id: 'A', status: 'green', statusText: 'Active', name: 'Zone A', detail: 'Measured 19.2 L/min, theoretical 20 L/min', flow: 96, flowClass: '' },
    { id: 'B', status: 'orange', statusText: 'Deviation', name: 'Zone B', detail: 'Measured 15.5 L/min, theoretical 20 L/min', flow: 78, flowClass: 'warn' },
    { id: 'D', status: 'red', statusText: 'Down', name: 'Zone D', detail: 'Measured 4.2 L/min, theoretical 18 L/min', flow: 24, flowClass: 'danger' },
    { id: 'F', status: 'primary', statusText: 'Inactive', name: 'Zone F', detail: 'Resting, next check at 17:00', flow: 0, flowClass: '' },
  ]

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Drip irrigation management</h1>
          <p>Measured flow rate, theoretical reference and intervention history.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Equipped zones</h2>
            <button className="ws-chip active" onClick={() => notify('Intervention log form ready.')}>Log intervention</button>
          </div>
          <div className="ws-panel-body">
            <div className="ws-zone-grid">
              {zones.map(zone => (
                <div className="ws-zone-card" key={zone.id}>
                  <span className={`ws-tag ${zone.status}`}>{zone.statusText}</span>
                  <h3>{zone.name}</h3>
                  <p className="ws-row-detail">{zone.detail}</p>
                  <div className={`ws-flow ${zone.flowClass}`}><span style={{ width: `${zone.flow}%` }}></span></div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Automatic alert</h2></div>
            <div className="ws-panel-body">
              <span className="ws-tag red">Abnormal flow</span>
              <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                Zone D has a 76% deviation. Inspect filter, lateral line and valve before next irrigation.
              </p>
            </div>
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Maintenance history</h2></div>
            <div className="ws-panel-body">
              <table className="ws-table">
                <tbody>
                  <tr><td>Aug 07</td><td>Filter cleaned, Zone B</td></tr>
                  <tr><td>Aug 02</td><td>Valve replaced, Zone D</td></tr>
                  <tr><td>Jul 29</td><td>Line flushed, Zone A</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}