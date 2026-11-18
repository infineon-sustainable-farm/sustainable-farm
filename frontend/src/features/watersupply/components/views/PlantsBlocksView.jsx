import { useMemo, useState } from 'react'
import { Unlink } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  IconButton,
  Input,
  Select,
  Table,
} from '../../../../shared/ui'
import { zoneBlockMappingApi } from '../../api/watersupplyApi'
import { useFarms, useFields, useZones } from '../../hooks/useFarms'
import { useBlockIrrigationNeed, useBlockRainfall, useZoneBlockMappings } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { ChoiceChips } from '../common/ChoiceChips'
import { FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { isoDay, monthStartDay } from './datetime'
import { sameId } from './ids'

/** Growth phases sent to Plants; the empty value lets Plants read its own calendar. */
const PHASES = [
  ['', 'calendar'],
  ['establishment', 'establishment'],
  ['gradual production', 'gradual'],
  ['full production', 'full'],
]

const PHASE_SOURCES = ['request', 'plants_calendar', 'unknown']

/** Same shape as Plants block codes ("A", "B2"): letters and digits, no prefix or space. */
const BLOCK_CODE_PATTERN = /^[A-Za-z0-9]{1,50}$/

/**
 * Plants blocks: links Water Supply zones to the Plants blocks they water, then shows what Plants
 * reads per block (rain gauge rainfall of the month, irrigation need of the day).
 *
 * Backend contract: ZoneBlockMapping { id (= zoneId), zoneId, blockCode, farmId }, read from the zone's
 * field (a block of the farm referential): linking gives the field its Plants code, unlinking clears it.
 * The farm is derived from the zone (zone -> field -> farm) because the API refuses a farm that does
 * not own the zone.
 */
export function PlantsBlocksView({ notify }) {
  const { t } = useTranslation('watersupply')
  const { mappings, loading, error, refetch } = useZoneBlockMappings()
  const { zones } = useZones()
  const { fields } = useFields()
  const { farms } = useFarms()
  const [form, setForm] = useState({ zoneId: '', blockCode: '' })
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)
  const [selectedBlock, setSelectedBlock] = useState('')

  const zoneNames = useMemo(() => new Map(zones.map((zone) => [zone.id, zone.name])), [zones])
  const zoneName = (zoneId) => zoneNames.get(zoneId) || t('plantsBlocks.zoneFallback', { id: String(zoneId).slice(0, 8) })
  const farmIdOfZone = (zoneId) => {
    const fieldId = zones.find((zone) => sameId(zone.id, zoneId))?.fieldId
    return fields.find((field) => sameId(field.id, fieldId))?.farmId
  }
  const farmName = (farmId) =>
    farms.find((farm) => farm.id === farmId)?.name || (farmId ? t('plantsBlocks.farmFallback', { id: String(farmId).slice(0, 8) }) : t('common.none'))

  const blockCodes = useMemo(() => [...new Set(mappings.map((mapping) => mapping.blockCode))].sort(), [mappings])
  const sortedMappings = useMemo(
    () => [...mappings].sort((a, b) => a.blockCode.localeCompare(b.blockCode)
      || String(zoneNames.get(a.zoneId) ?? '').localeCompare(String(zoneNames.get(b.zoneId) ?? ''))),
    [mappings, zoneNames],
  )
  // A zone listed under several blocks: Plants receives multipleBlockCodes=true for its volumes.
  const sharedZones = useMemo(() => {
    const counts = new Map()
    mappings.forEach((mapping) => counts.set(mapping.zoneId, (counts.get(mapping.zoneId) || 0) + 1))
    return new Set([...counts.entries()].filter(([, count]) => count > 1).map(([zoneId]) => zoneId))
  }, [mappings])
  const activeBlock = selectedBlock || blockCodes[0] || ''

  const updateForm = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = (event) => {
    event.preventDefault()
    if (!form.zoneId) {
      setFormError(t('plantsBlocks.errors.zoneRequired'))
      return
    }
    if (!BLOCK_CODE_PATTERN.test(form.blockCode.trim())) {
      setFormError(t('plantsBlocks.errors.codeFormat'))
      return
    }
    const farmId = farmIdOfZone(form.zoneId)
    if (!farmId) {
      setFormError(t('plantsBlocks.errors.farmUnknown'))
      return
    }
    setBusy(true)
    setFormError(null)
    zoneBlockMappingApi
      .createMapping({ zoneId: zones.find((zone) => sameId(zone.id, form.zoneId))?.id ?? form.zoneId, blockCode: form.blockCode.trim(), farmId })
      .then((created) => refetch().then(() => created))
      .then((created) => {
        notify(t('plantsBlocks.linked', { code: created?.blockCode ?? form.blockCode.trim().toUpperCase() }))
        setForm({ zoneId: '', blockCode: '' })
      })
      .catch((err) => setFormError(err.status === 409 ? err.message || t('plantsBlocks.errors.alreadyLinked') : err.message || t('common.saveError')))
      .finally(() => setBusy(false))
  }

  const unlink = (mapping) => {
    zoneBlockMappingApi
      .deleteMapping(mapping.id)
      .then(() => refetch())
      .then(() => notify(t('plantsBlocks.unlinked', { code: mapping.blockCode })))
      .catch((err) => notify(err.message || t('common.deleteError'), 'error'))
  }

  const columns = [
    { key: 'blockCode', header: t('plantsBlocks.columns.block'), cell: (mapping) => <Badge tone="info">{mapping.blockCode}</Badge> },
    {
      key: 'zone',
      header: t('plantsBlocks.columns.zone'),
      cell: (mapping) => (
        <span className="inline-flex flex-wrap items-center gap-2">
          {zoneName(mapping.zoneId)}
          {sharedZones.has(mapping.zoneId) && (
            <Badge tone="warning" title={t('plantsBlocks.severalBlocksHint')}>{t('plantsBlocks.severalBlocks')}</Badge>
          )}
        </span>
      ),
    },
    { key: 'farm', header: t('plantsBlocks.columns.farm'), cell: (mapping) => farmName(mapping.farmId) },
  ]

  function renderList() {
    if (loading) return <LoadingState label={t('plantsBlocks.loading')} />
    if (error) return <ErrorState message={error.message || t('plantsBlocks.loadError')} />
    if (mappings.length === 0) return <EmptyState title={t('plantsBlocks.emptyTitle')} message={t('plantsBlocks.empty')} />
    return (
      <Table
        caption={t('plantsBlocks.listTitle')}
        columns={columns}
        rows={sortedMappings}
        rowActions={(mapping) => (
          <div className="flex justify-end">
            <IconButton icon={Unlink} tone="danger" label={t('plantsBlocks.unlink')} onClick={() => unlink(mapping)} />
          </div>
        )}
      />
    )
  }

  return (
    <Screen title={t('plantsBlocks.title')} subtitle={t('plantsBlocks.subtitle')}>
      <SplitLayout
        main={
          <>
            <Card
              title={t('plantsBlocks.listTitle')}
              actions={
                <span className="text-sm text-text-muted">
                  {t('plantsBlocks.count', { blocks: blockCodes.length, links: mappings.length })}
                </span>
              }
            >
              {renderList()}
            </Card>
            {blockCodes.length > 0 && (
              <BlockDetails blockCodes={blockCodes} activeBlock={activeBlock} onSelect={setSelectedBlock} />
            )}
          </>
        }
        aside={
          <Card title={t('plantsBlocks.formTitle')}>
            <FormAlert>{formError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={submit}>
              <Field label={t('plantsBlocks.fields.zone')} required>
                <Select value={form.zoneId} onChange={updateForm('zoneId')}>
                  <option value="">{t('common.select')}</option>
                  {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
                </Select>
              </Field>
              <Field label={t('plantsBlocks.fields.blockCode')} required>
                <Input value={form.blockCode} onChange={updateForm('blockCode')} placeholder={t('plantsBlocks.fields.blockCodePlaceholder')} />
              </Field>
              <p className="text-xs text-text-muted">{t('plantsBlocks.codeHint')}</p>
              <FormActions>
                <Button type="submit" loading={busy}>{t('plantsBlocks.link')}</Button>
              </FormActions>
            </form>
          </Card>
        }
      />
    </Screen>
  )
}

/** One figure of the block: value (or a placeholder while loading or on error) and its label. */
function BlockFigure({ value, label }) {
  return (
    <div className="text-sm">
      {value}
      <p className="mt-1 text-xs font-medium text-text-muted">{label}</p>
    </div>
  )
}

/** What Plants reads for one block: rainfall of the current month and irrigation need of today. */
function BlockDetails({ blockCodes, activeBlock, onSelect }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const [phase, setPhase] = useState('')
  const today = isoDay()
  const { data: rainfall, loading: rainLoading, error: rainError } = useBlockRainfall(activeBlock, monthStartDay(), today)
  const { data: need, loading: needLoading, error: needError } = useBlockIrrigationNeed(activeBlock, phase, today)
  const pending = t('plantsBlocks.details.pending')
  const phaseSource = PHASE_SOURCES.includes(need?.growthPhaseSource)
    ? t(`plantsBlocks.details.phaseSources.${need.growthPhaseSource}`)
    : pending

  function figure(isLoading, failed, value) {
    if (isLoading) return pending
    if (failed) return t('common.none')
    return <strong className="font-semibold">{value}</strong>
  }

  return (
    <Card
      title={t('plantsBlocks.details.title', { code: activeBlock })}
      actions={
        <ChoiceChips
          label={t('plantsBlocks.details.blockChoice')}
          options={blockCodes.map((code) => ({ value: code, label: code }))}
          value={activeBlock}
          onChange={onSelect}
        />
      }
    >
      <div className="grid gap-4 sm:grid-cols-3">
        <BlockFigure
          value={figure(rainLoading, rainError, format.mm(rainfall?.rainfallMm ?? 0))}
          label={
            rainfall
              ? t('plantsBlocks.details.rainfallWithGauges', { count: rainfall.gaugeCount })
              : t('plantsBlocks.details.rainfall')
          }
        />
        <BlockFigure value={figure(needLoading, needError, format.liters(need?.needLiters ?? 0))} label={t('plantsBlocks.details.need')} />
        <BlockFigure
          value={
            need ? (
              <strong className="font-semibold">
                {t('plantsBlocks.details.phaseValue', {
                  phase: need.growthPhase || t('plantsBlocks.details.unknownPhase'),
                  factor: format.number(need.phaseFactor),
                })}
              </strong>
            ) : (
              t('common.none')
            )
          }
          label={t('plantsBlocks.details.phase', { source: phaseSource })}
        />
      </div>
      {(rainError || needError) && (
        <div className="mt-4">
          <FormAlert>{(rainError || needError).message}</FormAlert>
        </div>
      )}
      <div className="mt-4 max-w-xs">
        <Field label={t('plantsBlocks.details.phaseField')}>
          <Select value={phase} onChange={(event) => setPhase(event.target.value)}>
            {PHASES.map(([value, key]) => <option key={key} value={value}>{t(`plantsBlocks.details.phases.${key}`)}</option>)}
          </Select>
        </Field>
      </div>
      {need?.warnings?.includes('phase_factors_not_validated') && (
        <p className="mt-3 text-xs text-text-muted">
          {t('plantsBlocks.details.factorsNotValidated', {
            base: format.liters(need.baseNeedLiters),
            et0: format.mm(need.et0Mm),
          })}
        </p>
      )}
      {rainfall && <p className="mt-2 text-xs text-text-muted">{t('plantsBlocks.details.gaugesPerFarm')}</p>}
    </Card>
  )
}
