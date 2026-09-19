import { useMemo, useState } from 'react'
import { Check, CheckCheck, Trash2 } from 'lucide-react'
import { notificationApi } from '../../api/watersupplyApi'
import { useNotifications } from '../../hooks/useWaterData'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'

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
  const { notifications, loading, error, refetch } = useNotifications()
  const [typeFilter, setTypeFilter] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  const types = useMemo(() => [...new Set(notifications.map((item) => item.type).filter(Boolean))], [notifications])
  const typeFiltered = useMemo(
    () => notifications.filter((item) => !typeFilter || item.type === typeFilter),
    [notifications, typeFilter],
  )

  // Client-side search + pagination.
  const list = useListControls(typeFiltered, { searchFields: ['title', 'message', 'type'] })
  const filtered = list.items

  const run = (request, message) => {
    setBusy(true)
    request()
      .then(() => refetch())
      .then(() => notify(message))
      .catch((err) => notify(err.message || 'Action failed.'))
      .finally(() => setBusy(false))
  }

  const deleteNotification = () => {
    if (!deleteTarget) return
    run(() => notificationApi.deleteNotification(deleteTarget.id), 'Notification deleted')
    setDeleteTarget(null)
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Notification center</h1>
          <p>Operational alerts from quality, irrigation and storage checks.</p>
        </div>
      </div>

      <div className="ws-panel">
        <div className="ws-panel-header">
          <h2>Notifications</h2>
          <button className="ws-action-btn secondary" disabled={busy || notifications.length === 0}  onClick={() => run(notificationApi.markAllAsRead, 'Notifications marked as read')}>
            <CheckCheck size={15} /> Mark all as read
          </button>
        </div>
        <div className="ws-filters">
          <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            <option value="">All levels</option>
            {types.map((type) => <option key={type} value={type}>{type}</option>)}
          </select>
          <SearchInput value={list.query} onChange={list.setQuery} placeholder="Search a notification…" />
        </div>
        <div className="ws-panel-body">
          {loading ? (
            <Spinner label="Loading notifications..." full />
          ) : error ? (
            <EmptyState title="Error" description={error.message || 'Unable to load.'} />
          ) : filtered.length === 0 ? (
            <EmptyState title="No notifications" description="Alerts will appear here." />
          ) : (
            <div className="ws-summary-list">
              {filtered.map((item) => (
                <div className="ws-summary-item" key={item.id}>
                  <span className={`ws-summary-dot ${item.read ? 'green' : tagFor(item.type)}`} />
                  <div className="ws-summary-title">
                    <span className={`ws-tag ${tagFor(item.type)}`}>{item.type || 'info'}</span>
                    {item.title}
                    <small>{item.message}</small>
                  </div>
                  <div className="ws-actions">
                    {!item.read && (
                      <button className="ws-icon-btn" title="Mark as read" onClick={() => run(() => notificationApi.markAsRead(item.id), 'Notification marked as read')}>
                        <Check size={15} />
                      </button>
                    )}
                    <button className="ws-icon-btn danger" title="Delete" onClick={() => setDeleteTarget(item)}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
          <Pagination page={list.page} pageCount={list.pageCount} onPage={list.setPage} total={list.total} unit="notification" />
        </div>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Delete notification"
          message="This notification will be removed from the notification center."
          onConfirm={deleteNotification}
          onCancel={() => setDeleteTarget(null)}
          busy={busy}
        />
      )}
    </>
  )
}
