import { useState } from 'react'

export function IrrigationView({ notify, userName, initials }) {
  const [zones, setZones] = useState([
    { id: 'A', name: 'Zone A, Mango block 1', type: 'Drip network', time: '05:30 - 06:15', duration: '45 min, 1 200 L planned', status: 'green', statusText: 'Completed' },
    { id: 'B', name: 'Zone B, Mango block 2', type: 'Drip network', time: '06:15 - 07:00', duration: '45 min, 1 100 L planned', status: 'orange', statusText: 'In progress' },
    { id: 'C', name: 'Zone C, Nursery', type: 'Sprinkler', time: '17:00 - 17:30', duration: '30 min, 400 L planned', status: 'primary', statusText: 'Planned' },
    { id: 'D', name: 'Zone D, Mango block 3', type: 'Drip network', time: 'Postponed', duration: 'Next check tomorrow 05:30', status: 'red', statusText: 'Postponed', note: true },
    { id: 'E', name: 'Zone E, Processing yard', type: 'Manual line', time: '18:00 - 18:20', duration: '20 min, 250 L planned', status: 'primary', statusText: 'Planned' },
  ])

  const handleAction = (zone, action) => {
    setZones(prev => prev.map(z => {
      if (z.id !== zone.id) return z
      if (action === 'start') {
        return { ...z, status: 'orange', statusText: 'In progress' }
      }
      if (action === 'stop') {
        return { ...z, status: 'green', statusText: 'Completed' }
      }
      return z
    }))
    notify(action === 'start' ? `Irrigation started for ${zone.name}.` : `Irrigation stopped and recorded for ${zone.name}.`)
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Irrigation scheduling and automation</h1>
          <p>Irrigation runs when the soil needs it, not just because the clock says so.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div className="ws-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M7 20h10M10 20c5.5-2.5.8-6.4 3-10" />
              <path d="M9.5 9.4c1.1.8 1.8 2.2 2.3 3.7-2 .4-3.5.4-4.8-.3-1.2-.6-2.3-1.9-3-4.2 2.8-.5 4.4 0 5.5.8z" />
            </svg>
          </div>
          <div>
            <div className="label">Current season</div>
            <div className="value">Dry season, humidity low</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div className="ws-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 20h18M5 20V8a7 7 0 0 1 14 0v12" />
              <path d="M8 13h8" />
            </svg>
          </div>
          <div>
            <div className="label">Reservoir level</div>
            <div className="value">58%, above critical threshold</div>
          </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Today's irrigation plan</h2>
            <span>6 zones, updated 5 min ago</span>
          </div>
          {zones.map(zone => (
            <div className="ws-row" key={zone.id}>
              <div className="ws-row-title">{zone.name}<small>{zone.type}</small></div>
              <div className="ws-row-detail">{zone.time}<small>{zone.duration}</small></div>
              <span className={`ws-tag ${zone.status}`}>{zone.statusText}</span>
              <div className="ws-actions">
                {zone.status === 'primary' && (
                  <button className="ws-icon-btn" title="Start now" onClick={() => handleAction(zone, 'start')}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="6 3 20 12 6 21 6 3" /></svg>
                  </button>
                )}
                {zone.status === 'orange' && (
                  <button className="ws-icon-btn" title="Stop irrigation" onClick={() => handleAction(zone, 'stop')}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="14" y="4" width="4" height="16" rx="1" /><rect x="6" y="4" width="4" height="16" rx="1" /></svg>
                  </button>
                )}
                {zone.status === 'green' && (
                  <button className="ws-icon-btn" title="Completed" onClick={() => notify('Status confirmed.')}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 6 9 17l-5-5" /></svg>
                  </button>
                )}
              </div>
              {zone.note && (
                <div className="ws-note">Postponed automatically: soil humidity above seasonal threshold, watering was not needed today.</div>
              )}
            </div>
          ))}
        </div>

        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Season controls</h2></div>
            <div className="ws-panel-body">
              <table className="ws-table">
                <tbody>
                  <tr><td>Season</td><td><strong>Dry, vigilance</strong></td></tr>
                  <tr><td>Humidity threshold</td><td><strong>32%</strong></td></tr>
                  <tr><td>Critical threshold</td><td><strong>20%</strong></td></tr>
                  <tr><td>Postponed today</td><td><strong>1 zone</strong></td></tr>
                </tbody>
              </table>
            </div>
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Status legend</h2></div>
            <div className="ws-panel-body ws-stack">
              <span className="ws-tag primary">Planned</span>
              <span className="ws-tag orange">In progress</span>
              <span className="ws-tag green">Completed</span>
              <span className="ws-tag red">Postponed</span>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}