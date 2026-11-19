import { useMemo, useState } from 'react'
import { Check, CheckCheck, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, ConfirmDialog, EmptyState, ErrorState, IconButton, Select } from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { notificationApi } from '../../api/watersupplyApi'
import { useNotifications } from '../../hooks/useWaterData'
import { Filters, Screen } from '../common/Screen'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { SummaryItem, SummaryList } from '../common/SummaryList'
import { toneOf } from '../common/tones'

function tagFor(type) {
  switch ((type || '').toLowerCase()) {
    case 'critical':
    case 'warning':
      return 'red'
    case 'info':
      return 'primary'
    default:
      return 'green'
  }
}

export function NotificationsView({ notify }) {
  const { t } = useTranslation('watersupply')
  const { notifications, loading, error, refetch } = useNotifications()
  const [typeFilter, setTypeFilter] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  const types = useMemo(() => [...new Set(notifications.map((item) => item.type).filter(Boolean))], [notifications])
  const typeFiltered = useMemo(
    () => notifications.filter((item) => !typeFilter || item.type === typeFilter),
    [notifications, typeFilter],
  )
  const typeLabel = (type) => t(`notifications.types.${(type || 'info').toLowerCase()}`, { defaultValue: type || 'info' })

  // Client-side search + pagination.
  const list = useListControls(typeFiltered, { searchFields: ['title', 'message', 'type'] })
  const filtered = list.items

  const run = (request, message) => {
    setBusy(true)
    request()
      .then(() => refetch())
      .then(() => notify(message))
      .catch((err) => notify(err.message || t('notifications.actionFailed'), 'error'))
      .finally(() => setBusy(false))
  }

  const deleteNotification = () => {
    if (!deleteTarget) return
    run(() => notificationApi.deleteNotification(deleteTarget.id), t('notifications.deleted'))
    setDeleteTarget(null)
  }

  function renderList() {
    if (loading) return <LoadingState label={t('notifications.loading')} />
    if (error) return <ErrorState message={error.message || t('notifications.loadError')} />
    if (filtered.length === 0) return <EmptyState title={t('notifications.emptyTitle')} message={t('notifications.empty')} />
    return (
      <SummaryList>
        {filtered.map((item) => (
          <SummaryItem
            key={item.id}
            tone={toneOf(item.read ? 'green' : tagFor(item.type))}
            title={
              <span className="flex flex-wrap items-center gap-2">
                <Badge tone={toneOf(tagFor(item.type))}>{typeLabel(item.type)}</Badge>
                {item.title}
              </span>
            }
            detail={item.message}
            aside={
              <div className="flex gap-1">
                {!item.read && (
                  <IconButton
                    icon={Check}
                    label={t('notifications.markRead')}
                    onClick={() => run(() => notificationApi.markAsRead(item.id), t('notifications.markedRead'))}
                  />
                )}
                <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => setDeleteTarget(item)} />
              </div>
            }
          />
        ))}
      </SummaryList>
    )
  }

  return (
    <Screen title={t('notifications.title')} subtitle={t('notifications.subtitle')}>
      <Card
        title={t('notifications.listTitle')}
        actions={
          <Button
            variant="secondary"
            icon={CheckCheck}
            disabled={busy || notifications.length === 0}
            onClick={() => run(notificationApi.markAllAsRead, t('notifications.allMarkedRead'))}
          >
            {t('notifications.markAllRead')}
          </Button>
        }
      >
        <Filters>
          <Select aria-label={t('notifications.levelFilter')} value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} className="w-auto">
            <option value="">{t('notifications.allLevels')}</option>
            {types.map((type) => <option key={type} value={type}>{typeLabel(type)}</option>)}
          </Select>
          <SearchField value={list.query} onChange={list.setQuery} placeholder={t('notifications.search')} />
        </Filters>
        {renderList()}
        <ListPagination list={list} />
      </Card>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('notifications.deleteTitle')}
        message={t('notifications.deleteMessage')}
        onConfirm={deleteNotification}
        onClose={() => setDeleteTarget(null)}
        loading={busy}
      />
    </Screen>
  )
}
