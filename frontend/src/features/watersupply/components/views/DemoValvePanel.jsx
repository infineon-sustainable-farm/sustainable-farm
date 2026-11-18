import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, ErrorState, Input, Table } from '../../../../shared/ui'
import { iotCommandApi } from '../../api/watersupplyApi'
import { useValveCommands } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { LoadingState } from '../common/LoadingState'

const STATUS_TONES = { dispatched: 'success', queued: 'info', rejected: 'error' }

/** Short opening of the demonstration valve (the backend caps it, 30 s by default). */
const DEMO_OPENING_SECONDS = 10

/**
 * Demonstration valve: sends OPEN_VALVE / CLOSE_VALVE and shows the device's command journal.
 *
 * No device gateway exists yet, so the backend answers 503 VALVE_GATEWAY_UNAVAILABLE and records the
 * attempt as "rejected": the screen says so plainly instead of pretending the valve moved.
 */
export function DemoValvePanel({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const [deviceInput, setDeviceInput] = useState('')
  const [deviceId, setDeviceId] = useState('')
  const [busy, setBusy] = useState(false)
  const { commands, loading, error, refetch } = useValveCommands(deviceId)
  const seconds = format.number(DEMO_OPENING_SECONDS)

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
      .then(() => notify(command === 'OPEN_VALVE' ? t('valves.opened', { seconds }) : t('valves.closed')))
      .catch((err) => {
        if (err.status === 503) {
          notify(t('valves.errors.noGateway'), 'warning')
        } else if (err.status === 409) {
          notify(err.message || t('valves.errors.offline'), 'error')
        } else if (err.status === 404) {
          notify(t('valves.errors.unknownDevice'), 'error')
        } else {
          notify(err.message || t('valves.errors.failed'), 'error')
        }
      })
      // The journal is refreshed in every case: a refused attempt is part of the audit trail.
      .finally(() => {
        setBusy(false)
        refetch()
      })
  }

  const columns = [
    { key: 'requestedAt', header: t('valves.columns.requested'), cell: (command) => format.instant(command.requestedAt) },
    {
      key: 'command',
      header: t('valves.columns.command'),
      cell: (command) => (command.command === 'OPEN_VALVE' ? t('valves.commands.open') : t('valves.commands.close')),
    },
    {
      key: 'status',
      header: t('valves.columns.status'),
      cell: (command) => (
        <Badge tone={STATUS_TONES[command.status] ?? 'info'}>
          {t(`valves.status.${command.status}`, { defaultValue: command.status })}
        </Badge>
      ),
    },
  ]

  function renderJournal() {
    if (!deviceId) return <EmptyState title={t('valves.noDeviceTitle')} message={t('valves.noDevice')} />
    if (loading) return <LoadingState compact label={t('valves.loading')} />
    if (error) return <ErrorState message={error.message || t('valves.loadError')} />
    if (commands.length === 0) {
      return <EmptyState title={t('valves.emptyTitle')} message={t('valves.empty', { deviceId })} />
    }
    return <Table caption={t('valves.journalCaption')} columns={columns} rows={commands} getRowKey={(command) => command.commandId} />
  }

  const noDevice = !deviceInput.trim()

  return (
    <Card title={t('valves.title')} actions={<Badge tone="warning">{t('valves.gatewayMissing')}</Badge>}>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Input
          className="max-w-56"
          value={deviceInput}
          onChange={(event) => setDeviceInput(event.target.value)}
          placeholder={t('valves.devicePlaceholder')}
          aria-label={t('valves.deviceLabel')}
        />
        <Button disabled={busy || noDevice} onClick={() => send('OPEN_VALVE')}>{t('valves.open', { seconds })}</Button>
        <Button variant="secondary" disabled={busy || noDevice} onClick={() => send('CLOSE_VALVE')}>{t('valves.close')}</Button>
        <Button variant="ghost" disabled={noDevice} onClick={() => setDeviceId(deviceInput.trim())}>{t('valves.showJournal')}</Button>
      </div>
      <p className="mb-3 text-xs text-text-muted">{t('valves.explanation')}</p>
      {renderJournal()}
    </Card>
  )
}
