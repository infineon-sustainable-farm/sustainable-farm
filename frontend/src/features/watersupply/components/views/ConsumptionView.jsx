import { useState } from 'react'

export function ConsumptionView({ notify, userName, initials }) {
  const [activeFunction, setActiveFunction] = useState('Irrigation')
  const [activePeriod, setActivePeriod] = useState('Week')

  const functions = ['Irrigation', 'Washing', 'Processing', 'Dryer cleaning']
  const periods = ['Day', 'Week', 'Month']

  const chartData = {
    Irrigation: [
      { day: 'Mon', ref: 62, actual: 52 },
      { day: 'Tue', ref: 72, actual: 61 },
      { day: 'Wed', ref: 65, actual: 49 },
      { day: 'Thu', ref: 78, actual: 64 },
      { day: 'Fri', ref: 70, actual: 57 },
      { day: 'Sat', ref: 48, actual: 44 },
    ],
    Washing: [
      { day: 'Mon', ref: 30, actual: 34 },
      { day: 'Tue', ref: 28, actual: 31 },
      { day: 'Wed', ref: 35, actual: 38 },
      { day: 'Thu', ref: 32, actual: 36 },
      { day: 'Fri', ref: 40, actual: 42 },
      { day: 'Sat', ref: 25, actual: 28 },
    ],
    Processing: [
      { day: 'Mon', ref: 50, actual: 45 },
      { day: 'Tue', ref: 55, actual: 48 },
      { day: 'Wed', ref: 48, actual: 44 },
      { day: 'Thu', ref: 60, actual: 52 },
      { day: 'Fri', ref: 52, actual: 47 },
      { day: 'Sat', ref: 40, actual: 38 },
    ],
    'Dryer cleaning': [
      { day: 'Mon', ref: 20, actual: 22 },
      { day: 'Tue', ref: 18, actual: 19 },
      { day: 'Wed', ref: 22, actual: 24 },
      { day: 'Thu', ref: 19, actual: 20 },
      { day: 'Fri', ref: 25, actual: 26 },
      { day: 'Sat', ref: 15, actual: 16 },
    ],
  }

  const data = chartData[activeFunction] || chartData.Irrigation

  const handleFunctionChange = (fn) => {
    setActiveFunction(fn)
    notify(`Filter applied: ${fn}`)
  }

  const handlePeriodChange = (period) => {
    setActivePeriod(period)
    notify(`Period changed: ${period}`)
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water consumption tracking</h1>
          <p>Actual consumption compared with the theoretical reference.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div>
            <div className="label">Selected period</div>
            <div className="value">This {activePeriod.toLowerCase()}</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Water saved</div>
            <div className="value" style={{ color: 'var(--ws-primary-dark)' }}>18%</div>
          </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Consumption over time</h2>
            <div className="ws-switcher">
              {functions.map(fn => (
                <button
                  key={fn}
                  className={`ws-chip ${activeFunction === fn ? 'active' : ''}`}
                  onClick={() => handleFunctionChange(fn)}
                >
                  {fn}
                </button>
              ))}
            </div>
          </div>
          <div className="ws-panel-body">
            <div className="ws-switcher">
              {periods.map(period => (
                <button
                  key={period}
                  className={`ws-chip ${activePeriod === period ? 'active' : ''}`}
                  onClick={() => handlePeriodChange(period)}
                >
                  {period}
                </button>
              ))}
              <button className="ws-chip">Point: Main reservoir</button>
            </div>
            <div className="ws-chart">
              {data.map(item => (
                <div className="ws-bar-pair" key={item.day}>
                  <div className="ws-bar ref" style={{ height: `${item.ref}%` }}></div>
                  <div className="ws-bar" style={{ height: `${item.actual}%` }}></div>
                  <span>{item.day}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Volume by function</h2>
            <span>Actual vs reference</span>
          </div>
          <div className="ws-panel-body">
            <table className="ws-table">
              <thead>
                <tr>
                  <th>Function</th>
                  <th>Actual</th>
                  <th>Reference</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr><td>Irrigation</td><td>8 420 L</td><td>10 200 L</td><td><span className="ws-tag green">Saved</span></td></tr>
                <tr><td>Washing</td><td>1 180 L</td><td>1 100 L</td><td><span className="ws-tag orange">Watch</span></td></tr>
                <tr><td>Processing</td><td>2 240 L</td><td>2 480 L</td><td><span className="ws-tag green">Saved</span></td></tr>
                <tr><td>Dryer cleaning</td><td>460 L</td><td>450 L</td><td><span className="ws-tag primary">Stable</span></td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  )
}