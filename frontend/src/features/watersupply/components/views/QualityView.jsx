export function QualityView({ notify, userName, initials }) {
  const tests = [
    { date: 'Aug 08', source: 'Reservoir', ph: '8.7', phClass: 'red', turbidity: '4.8 NTU', turbidityClass: '', status: 'red', statusText: 'Non-compliant' },
    { date: 'Aug 08', source: 'Rainwater', ph: '7.1', phClass: '', turbidity: '5.2 NTU', turbidityClass: 'orange', status: 'orange', statusText: 'Review' },
    { date: 'Aug 06', source: 'Borehole', ph: '7.3', phClass: '', turbidity: '2.1 NTU', turbidityClass: '', status: 'green', statusText: 'Compliant' },
    { date: 'Aug 03', source: 'Reservoir', ph: '7.5', phClass: '', turbidity: '2.8 NTU', turbidityClass: '', status: 'green', statusText: 'Compliant' },
  ]

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water quality testing logs</h1>
          <p>Test results sorted by date with out-of-range values surfaced first.</p>
        </div>
        <div className="ws-user-chip">
          <span>{userName}</span>
          <div className="ws-avatar">{initials}</div>
        </div>
      </div>

      <div className="ws-panel">
        <div className="ws-panel-header">
          <h2>Recent tests</h2>
          <button className="ws-chip active" onClick={() => notify('New test form ready.')}>Record new test</button>
        </div>
        <div className="ws-panel-body">
          <table className="ws-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Source</th>
                <th>pH</th>
                <th>Turbidity</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {tests.map((test, i) => (
                <tr key={i}>
                  <td>{test.date}</td>
                  <td>{test.source}</td>
                  <td><strong style={{ color: test.phClass ? `var(--ws-${test.phClass})` : 'inherit' }}>{test.ph}</strong></td>
                  <td>{test.turbidityClass ? <strong style={{ color: `var(--ws-${test.turbidityClass})` }}>{test.turbidity}</strong> : test.turbidity}</td>
                  <td><span className={`ws-tag ${test.status}`}>{test.statusText}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}