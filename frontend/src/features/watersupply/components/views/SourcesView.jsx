import { useMemo, useState } from 'react'
import { History, Pencil, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Field,
  IconButton,
  Input,
  Select,
  Table,
} from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { waterSourceApi } from '../../api/watersupplyApi'
import { useFarms } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useWaterFormat } from '../../format'
import { Filters, FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { tableSort } from '../common/listSort'
import { LevelHistoryPanel } from './LevelHistoryPanel'
import { sameId } from './ids'

const SOURCE_TYPES = ['borehole', 'rain', 'river', 'well', 'municipal']
const EMPTY_FORM = { farmId: '', name: '', type: 'borehole', capacityLiters: '', currentLevelLiters: '' }

/** Level tone: orange when nearly full, green when there is water, red when empty. */
function levelTone(pct) {
  if (pct >= 90) return 'warning'
  return pct > 0 ? 'success' : 'error'
}

/**
 * Water sources view: full CRUD with farm association and validation.
 * Backend contract: WaterSource { farmId, name, type, capacityLiters, currentLevelLiters }.
 */
export function SourcesView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { sources, loading, error, refetch } = useWaterSources()
  const { farms } = useFarms()
  const [farmFilter, setFarmFilter] = useState('')
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [historyTarget, setHistoryTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)
  const [deleteError, setDeleteError] = useState(null)

  const farmName = (farmId) =>
    farms.find((f) => f.id === farmId)?.name || (farmId ? t('sources.farmFallback', { id: String(farmId).slice(0, 8) }) : t('common.none'))
  const typeLabel = (type) => (type ? t(`sources.types.${type}`, { defaultValue: type }) : t('common.none'))

  const farmFiltered = useMemo(
    () => sources.filter((item) => !farmFilter || sameId(item.farmId, farmFilter)),
    [sources, farmFilter],
  )

  // Client-side search + sort + pagination.
  const list = useListControls(farmFiltered, {
    searchFields: ['name', 'type'],
    defaultSort: { key: 'name', dir: 'asc' },
  })

  const updateForm = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const resetForm = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  const submitSource = (event) => {
    event.preventDefault()
    if (!form.farmId) {
      setFormError(t('sources.errors.farmRequired'))
      return
    }
    if (!form.name.trim()) {
      setFormError(t('sources.errors.nameRequired'))
      return
    }
    const capacity = Number(form.capacityLiters)
    if (!(capacity > 0)) {
      setFormError(t('sources.errors.capacityPositive'))
      return
    }
    const level = form.currentLevelLiters === '' ? 0 : Number(form.currentLevelLiters)
    if (level < 0 || level > capacity) {
      setFormError(t('sources.errors.levelRange'))
      return
    }
    setBusy(true)
    setFormError(null)
    const payload = {
      farmId: form.farmId,
      name: form.name.trim(),
      type: form.type,
      capacityLiters: capacity,
      currentLevelLiters: level,
    }
    const request = editingId ? waterSourceApi.updateSource(editingId, payload) : waterSourceApi.createSource(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingId ? t('sources.updated') : t('sources.created'))
        resetForm()
      })
      .catch((err) => setFormError(err.message || t('common.saveError')))
      .finally(() => setBusy(false))
  }

  const editSource = (item) => {
    setEditingId(item.id)
    setForm({
      farmId: item.farmId || '',
      name: item.name || '',
      type: item.type || 'borehole',
      capacityLiters: item.capacityLiters ?? '',
      currentLevelLiters: item.currentLevelLiters ?? '',
    })
    setFormError(null)
  }

  const deleteSource = () => {
    if (!deleteTarget) return
    setBusy(true)
    setDeleteError(null)
    waterSourceApi
      .deleteSource(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify(t('sources.deleted'))
        setDeleteTarget(null)
      })
      .catch((err) => setDeleteError(err.message || t('common.deleteError')))
      .finally(() => setBusy(false))
  }

  const columns = [
    { key: 'name', header: t('sources.columns.name'), sortable: true },
    { key: 'farm', header: t('sources.columns.farm'), cell: (item) => farmName(item.farmId) },
    { key: 'type', header: t('sources.columns.type'), cell: (item) => <Badge tone="info">{typeLabel(item.type)}</Badge> },
    { key: 'capacity', header: t('sources.columns.capacity'), cell: (item) => format.liters(item.capacityLiters ?? 0) },
    {
      key: 'level',
      header: t('sources.columns.level'),
      cell: (item) => {
        const pct = item.capacityLiters ? Math.round((item.currentLevelLiters / item.capacityLiters) * 100) : 0
        return (
          <Badge tone={levelTone(pct)}>
            {t('sources.levelValue', { level: format.liters(item.currentLevelLiters ?? 0), pct: format.percent(pct) })}
          </Badge>
        )
      },
    },
  ]

  function renderList() {
    if (loading) return <LoadingState label={t('sources.loading')} />
    if (error) return <ErrorState message={error.message || t('sources.loadError')} />
    if (farmFiltered.length === 0) return <EmptyState title={t('sources.emptyTitle')} message={t('sources.empty')} />
    return (
      <>
        <Table
          caption={t('sources.listTitle')}
          columns={columns}
          rows={list.items}
          {...tableSort(list)}
          rowActions={(item) => (
            <div className="flex justify-end gap-1">
              <IconButton icon={History} label={t('common.history')} onClick={() => setHistoryTarget(item)} />
              <IconButton icon={Pencil} label={t('common.edit')} onClick={() => editSource(item)} />
              <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => setDeleteTarget(item)} />
            </div>
          )}
        />
        <ListPagination list={list} />
      </>
    )
  }

  return (
    <Screen title={t('sources.title')} subtitle={t('sources.subtitle')}>
      <SplitLayout
        main={
          <>
            <Card
              title={t('sources.listTitle')}
              actions={<span className="text-sm text-text-muted">{t('sources.count', { count: list.total })}</span>}
            >
              <Filters>
                <Select
                  aria-label={t('sources.farmFilter')}
                  value={farmFilter}
                  onChange={(event) => setFarmFilter(event.target.value)}
                  className="w-auto"
                >
                  <option value="">{t('common.allFarms')}</option>
                  {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
                </Select>
                <SearchField value={list.query} onChange={list.setQuery} placeholder={t('sources.search')} />
              </Filters>
              {renderList()}
            </Card>
            {historyTarget && <LevelHistoryPanel source={historyTarget} onClose={() => setHistoryTarget(null)} />}
          </>
        }
        aside={
          <Card title={editingId ? t('sources.editTitle') : t('sources.newTitle')}>
            <FormAlert>{formError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={submitSource}>
              <Field label={t('sources.fields.farm')} required>
                <Select value={form.farmId} onChange={updateForm('farmId')}>
                  <option value="">{t('common.select')}</option>
                  {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
                </Select>
              </Field>
              <Field label={t('sources.fields.name')} required>
                <Input value={form.name} onChange={updateForm('name')} />
              </Field>
              <Field label={t('sources.fields.type')} required>
                <Select value={form.type} onChange={updateForm('type')}>
                  {SOURCE_TYPES.map((type) => <option key={type} value={type}>{typeLabel(type)}</option>)}
                </Select>
              </Field>
              <Field label={t('sources.fields.capacity')} required>
                <Input type="number" min="0" step="0.01" value={form.capacityLiters} onChange={updateForm('capacityLiters')} />
              </Field>
              <FormActions>
                {editingId && <Button variant="secondary" onClick={resetForm}>{t('common.cancel')}</Button>}
                <Button type="submit" loading={busy}>{t('common.save')}</Button>
              </FormActions>
            </form>
          </Card>
        }
      />
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('sources.deleteTitle', { name: deleteTarget?.name || t('sources.theSource') })}
        message={t('sources.deleteMessage')}
        onConfirm={deleteSource}
        onClose={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
        loading={busy}
        error={deleteError}
      />
    </Screen>
  )
}
