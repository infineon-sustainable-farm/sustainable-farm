// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { renderView as render } from './testRender'
import { NotificationsView } from './NotificationsView'
import { notificationApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  notificationApi: {
    getNotifications: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    deleteNotification: vi.fn(),
  },
}))

const notify = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  notificationApi.getNotifications.mockResolvedValue([
    { id: 'n1', type: 'warning', title: 'Low tank', message: 'Main tank under 20%', read: false },
    { id: 'n2', type: 'info', title: 'Irrigation done', message: 'Zone A watered', read: true },
  ])
})

describe('NotificationsView', () => {
  it('lists the notifications with their level', async () => {
    render(<NotificationsView notify={notify} />)

    expect(await screen.findByText('Low tank')).toBeTruthy()
    expect(screen.getByText('Main tank under 20%')).toBeTruthy()
    expect(screen.getAllByText('warning').length).toBeGreaterThan(0)
  })

  it('filters by level', async () => {
    render(<NotificationsView notify={notify} />)
    await screen.findByText('Low tank')

    fireEvent.change(screen.getByRole('combobox', { name: 'Filter by level' }), { target: { value: 'info' } })

    expect(screen.queryByText('Low tank')).toBeNull()
    expect(screen.getByText('Irrigation done')).toBeTruthy()
  })

  it('marks one notification as read, only the unread ones offer it', async () => {
    notificationApi.markAsRead.mockResolvedValue(undefined)
    render(<NotificationsView notify={notify} />)
    await screen.findByText('Low tank')

    const buttons = screen.getAllByRole('button', { name: 'Mark as read' })
    expect(buttons).toHaveLength(1)
    fireEvent.click(buttons[0])

    await waitFor(() => expect(notificationApi.markAsRead).toHaveBeenCalledWith('n1'))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Notification marked as read'))
  })

  it('deletes a notification after confirmation in the dialog', async () => {
    notificationApi.deleteNotification.mockResolvedValue(undefined)
    render(<NotificationsView notify={notify} />)
    await screen.findByText('Low tank')

    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0])
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('This notification will be removed from the notification center.')).toBeTruthy()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(notificationApi.deleteNotification).toHaveBeenCalledWith('n1'))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Notification deleted'))
    await waitFor(() => expect(notificationApi.getNotifications).toHaveBeenCalledTimes(2))
  })

  it('keeps the notification when the dialog is cancelled', async () => {
    render(<NotificationsView notify={notify} />)
    await screen.findByText('Low tank')

    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0])
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(notificationApi.deleteNotification).not.toHaveBeenCalled()
  })
})
