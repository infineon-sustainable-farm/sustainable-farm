import { useState } from 'react'
import { iotCommandApi } from '../../api/watersupplyApi'
import { useValveCommands } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { inputStyle } from '../../../../shared/components/formStyles'
import { EmptyState } from './EmptyState'
import { formatInstant } from './datetime'

const STATUS_META = {
  dispatched: { cls: 'green', text: 'Sent' },
  queued: { cls: 'primary', text: 'Queued' },
  rejected: { cls: 'red', text: 'Not sent' },
}

/** Short opening of the demonstration valve (the backend caps it, 30 s by default). */
const DEMO_OPENING_SECONDS = 10

/**
 * Demonstration valve: sends OPEN_VALVE / CLOSE_VALVE and shows the device's command journal.
 *
 * No device gateway exists yet, so the backend answers 503 VALVE_GATEWAY_UNAVAILABLE and records the
 * attempt as "rejected": the screen says so plainly instead of pretending the valve moved.
 */
export function DemoValvePanel({ notify }) {
  const [deviceInput, setDeviceInput] = useState('')
  const [deviceId, setDeviceId] = useState('')
  const [busy, setBusy] = useState(false)
  const { commands, loading, error, refetch } = useValveCommands(deviceId)

  const send = (command) => {
    const target = deviceInput.trim()
    if (!target) return
    setDeviceId(target)
    setBusy(true)
    const body = command === 'OPEN_VALVE'
      ? { command, durationSeconds: DEMO_OPENING_SECONDS, reason: 'visitor_demo' }
      : { command, reason: 'visitor_demo' }
    iotCommandApi
      .sendCommand(target, body)
      .then(() => notify(command === 'OPEN_VALVE' ? `Valve opened for ${DEMO_OPENING_SECONDS} s` : 'Valve closed'))
      .catch((err) => {
        if (err.status === 503) {
          notify('No device gateway yet: the command was recorded but not sent.')
        } else if (err.status === 409) {
          notify(err.message || 'The device is offline or a water outage is in force.')
        } else if (err.status === 404) {
          notify('Unknown device: it registers itself with its first telemetry.')
        } else {
          notify(err.message || 'Command failed.')
        }
      })
      // The journal is refreshed in every case: a refused attempt is part of the audit trail.
      .finally(() => {
        setBusy(false)
        refetch()
      })
  }

  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Demonstration valve</h2>
        <span className="ws-tag orange">Gateway not configured</span>
      </div>
      <div className="ws-filters">
        <input
          style={{ ...inputStyle, maxWidth: '220px' }}
          value={deviceInput}
          onChange={(event) => setDeviceInput(event.target.value)}
          placeholder="Device id, e.g. valve-demo-01"
          aria-label="Valve device id"
        />
        <button className="ws-action-btn" disabled={busy || !deviceInput.trim()} onClick={() => send('OPEN_VALVE')}>
          Open {DEMO_OPENING_SECONDS} s
        </button>
        <button className="ws-action-btn secondary" disabled={busy || !deviceInput.trim()} onClick={() => send('CLOSE_VALVE')}>
          Close
        </button>
        <button className="ws-chip" disabled={!deviceInput.trim()} onClick={() => setDeviceId(deviceInput.trim())}>
          Show journal
        </button>
      </div>
      <div className="ws-panel-body">
        <p style={{ margin: '0 0 12px', fontSize: '12px', color: 'var(--ws-muted)' }}>
          Openings are refused while a planned outage is in force; closing is always allowed. Until IoT
          connects a gateway, commands are journaled but never reach the valve.
        </p>
        {!deviceId ? (
          <EmptyState title="No device selected" description="Enter the valve's device id to see its command journal." />
        ) : loading ? (
          <Spinner label="Loading the journal…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load the journal.'} />
        ) : commands.length === 0 ? (
          <EmptyState title="No command yet" description={`Nothing was ever sent to ${deviceId}.`} />
        ) : (
          <table className="ws-table">
            <thead>
              <tr>
                <th>Requested</th>
                <th>Command</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {commands.map((command) => {
                const status = STATUS_META[command.status] || { cls: 'primary', text: command.status }
                return (
                  <tr key={command.commandId}>
                    <td>{formatInstant(command.requestedAt)}</td>
                    <td>{command.command === 'OPEN_VALVE' ? 'Open' : 'Close'}</td>
                    <td><span className={`ws-tag ${status.cls}`}>{status.text}</span></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
