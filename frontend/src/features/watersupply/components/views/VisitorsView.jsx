import { useState } from 'react'
import { Trans, useTranslation } from 'react-i18next'
import { Badge, Button, Card, ErrorState, Field, Input, Select, Textarea } from '../../../../shared/ui'
import { visitorApi } from '../../api/watersupplyApi'
import { useZones } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useSafetyBriefing, useVisitorSummary } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { Filters, FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { Stat } from '../common/Stat'
import { DemoValvePanel } from './DemoValvePanel'

/** One entry per line in the form; the API takes lists. */
const toLines = (list) => (list || []).join('\n')
const fromLines = (text) => text.split('\n').map((line) => line.trim()).filter(Boolean)

/**
 * Visitors: what the water team publishes for Visitor Management and what it shows.
 *
 * The visitor screens themselves belong to Visitor Management: here the water team writes the
 * safety briefing, previews the data a visitor screen receives, and drives the demonstration valve.
 */
export function VisitorsView({ notify }) {
  const { t } = useTranslation('watersupply')
  return (
    <Screen title={t('visitors.title')} subtitle={t('visitors.subtitle')}>
      <SplitLayout
        main={<SafetyBriefingPanel notify={notify} />}
        aside={
          <>
            <VisitorPreviewPanel />
            <DemoValvePanel notify={notify} />
          </>
        }
      />
    </Screen>
  )
}

function SafetyBriefingPanel({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { briefing, loading, error, refetch } = useSafetyBriefing()
  const [draft, setDraft] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const startEditing = () => {
    setDraft({
      version: '',
      text: briefing?.text || '',
      allowedZones: toLines(briefing?.allowedZones),
      forbiddenZones: toLines(briefing?.forbiddenZones),
      risks: toLines(briefing?.risks),
      maxCapacity: briefing?.maxCapacity ?? '',
      guideRequired: briefing?.guideRequired ?? true,
    })
    setFormError(null)
  }
  const updateDraft = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    setDraft((current) => ({ ...current, [key]: value }))
  }

  const publish = (event) => {
    event.preventDefault()
    const capacity = draft.maxCapacity === '' ? null : Number(draft.maxCapacity)
    if (capacity !== null && !(capacity >= 1)) {
      setFormError(t('visitors.briefing.errors.groupSize'))
      return
    }
    setBusy(true)
    setFormError(null)
    visitorApi
      .publishSafetyBriefing({
        // Empty version: the backend dates the new version itself.
        version: draft.version.trim() || null,
        text: draft.text,
        allowedZones: fromLines(draft.allowedZones),
        forbiddenZones: fromLines(draft.forbiddenZones),
        risks: fromLines(draft.risks),
        maxCapacity: capacity,
        guideRequired: draft.guideRequired,
      })
      .then(() => refetch())
      .then(() => {
        notify(t('visitors.briefing.published'))
        setDraft(null)
      })
      .catch((err) => setFormError(err.message || t('visitors.briefing.errors.publishFailed')))
      .finally(() => setBusy(false))
  }

  function renderContent() {
    if (loading) return <LoadingState label={t('visitors.briefing.loading')} />
    if (error) return <ErrorState message={error.message || t('visitors.briefing.loadError')} />
    if (draft) {
      return (
        <>
          <FormAlert>{formError}</FormAlert>
          <form className="flex flex-col gap-4" onSubmit={publish}>
            <Field label={t('visitors.briefing.fields.version')}>
              <Input value={draft.version} onChange={updateDraft('version')} />
            </Field>
            <Field label={t('visitors.briefing.fields.text')}>
              <Textarea rows={4} value={draft.text} onChange={updateDraft('text')} />
            </Field>
            <Field label={t('visitors.briefing.fields.allowed')}>
              <Textarea value={draft.allowedZones} onChange={updateDraft('allowedZones')} />
            </Field>
            <Field label={t('visitors.briefing.fields.forbidden')}>
              <Textarea value={draft.forbiddenZones} onChange={updateDraft('forbiddenZones')} />
            </Field>
            <Field label={t('visitors.briefing.fields.risks')}>
              <Textarea value={draft.risks} onChange={updateDraft('risks')} />
            </Field>
            <Field label={t('visitors.briefing.fields.maxCapacity')}>
              <Input type="number" min="1" step="1" value={draft.maxCapacity} onChange={updateDraft('maxCapacity')} />
            </Field>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="size-4 accent-primary" checked={draft.guideRequired} onChange={updateDraft('guideRequired')} />
              {t('visitors.briefing.fields.guideRequired')}
            </label>
            <FormActions>
              <Button variant="secondary" onClick={() => setDraft(null)}>{t('common.cancel')}</Button>
              <Button type="submit" loading={busy}>{t('visitors.briefing.publish')}</Button>
            </FormActions>
          </form>
        </>
      )
    }
    if (!briefing) return null
    return (
      <div className="flex flex-col gap-4 text-sm">
        {briefing.text && <p>{briefing.text}</p>}
        <BriefingList title={t('visitors.briefing.allowed')} items={briefing.allowedZones} tone="success" />
        <BriefingList title={t('visitors.briefing.forbidden')} items={briefing.forbiddenZones} tone="error" />
        <BriefingList title={t('visitors.briefing.risks')} items={briefing.risks} tone="warning" />
        <p>
          <Trans
            t={t}
            i18nKey={briefing.guideRequired ? 'visitors.briefing.maxGroupGuided' : 'visitors.briefing.maxGroup'}
            values={{ value: briefing.maxCapacity == null ? t('common.none') : format.number(briefing.maxCapacity) }}
            components={{ strong: <strong /> }}
          />
        </p>
        {briefing.publishedAt && (
          <p className="text-xs text-text-muted">{t('visitors.briefing.publishedAt', { date: format.instant(briefing.publishedAt) })}</p>
        )}
        <FormActions>
          <Button onClick={startEditing}>{t('visitors.briefing.newVersion')}</Button>
        </FormActions>
      </div>
    )
  }

  return (
    <Card
      title={t('visitors.briefing.title')}
      actions={
        briefing && (
          <Badge tone={briefing.validated ? 'success' : 'warning'}>
            {briefing.validated ? t('visitors.briefing.version', { version: briefing.version }) : t('visitors.briefing.notValidated')}
          </Badge>
        )
      }
    >
      {renderContent()}
    </Card>
  )
}

function BriefingList({ title, items, tone }) {
  if (!items?.length) return null
  return (
    <div>
      <p className="mb-2 text-xs font-semibold">{title}</p>
      <div className="flex flex-wrap gap-2">
        {items.map((item) => <Badge key={item} tone={tone}>{item}</Badge>)}
      </div>
    </div>
  )
}

/** Exactly what Visitor Management receives from GET /api/water-supply/visitor/summary. */
function VisitorPreviewPanel() {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { sources } = useWaterSources()
  const { zones } = useZones()
  const [sourceId, setSourceId] = useState('')
  const [zoneId, setZoneId] = useState('')
  const { summary, loading, error } = useVisitorSummary(sourceId, zoneId)

  const level = summary?.sourceLevel
  const irrigation = summary?.irrigationStatus
  const live = summary?.liveConsumption
  const outages = summary?.activeUnavailability ?? []

  function renderContent() {
    if (loading) return <LoadingState compact label={t('visitors.preview.loading')} />
    if (error) return <ErrorState message={error.message || t('visitors.preview.loadError')} />
    if (!summary) return null
    return (
      <div className="flex flex-col gap-4 text-sm">
        <div className="grid gap-4 sm:grid-cols-3">
          <Stat
            label={level ? t('visitors.preview.level', { name: level.name }) : t('visitors.preview.selectTank')}
            value={level && level.levelPercentage != null ? format.percent(level.levelPercentage) : t('common.none')}
          />
          <Stat
            label={t('visitors.preview.saved')}
            value={summary.waterSavingsPercentage == null ? t('common.none') : format.percent(summary.waterSavingsPercentage)}
          />
          <Stat
            label={t('visitors.preview.used', { minutes: format.number(live?.windowMinutes ?? 15) })}
            value={format.liters(live?.liters ?? 0)}
          />
        </div>
        {irrigation && (
          <p className="flex flex-wrap items-center gap-2">
            <Badge tone={irrigation.active ? 'warning' : 'info'}>
              {irrigation.active ? t('visitors.preview.irrigating') : t('visitors.preview.notIrrigating')}
            </Badge>
            {irrigation.lastActivationAt && t('visitors.preview.lastStarted', { date: format.instant(irrigation.lastActivationAt) })}
          </p>
        )}
        {outages.length > 0 && (
          <FormAlert>
            {t(zoneId || sourceId ? 'visitors.preview.outagesHere' : 'visitors.preview.outages', { count: format.number(outages.length) })}
          </FormAlert>
        )}
        {summary.waterSavingsPercentage == null && (
          <p className="text-xs text-text-muted">{t('visitors.preview.savingsUnavailable')}</p>
        )}
      </div>
    )
  }

  return (
    <Card title={t('visitors.preview.title')}>
      <Filters>
        <Select className="w-auto" value={sourceId} onChange={(event) => setSourceId(event.target.value)} aria-label={t('visitors.preview.tankShown')}>
          <option value="">{t('visitors.preview.noTank')}</option>
          {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
        </Select>
        <Select className="w-auto" value={zoneId} onChange={(event) => setZoneId(event.target.value)} aria-label={t('visitors.preview.zoneShown')}>
          <option value="">{t('visitors.preview.wholeSite')}</option>
          {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
        </Select>
      </Filters>
      {renderContent()}
    </Card>
  )
}
