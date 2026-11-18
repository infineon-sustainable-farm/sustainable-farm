// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderView as render } from './testRender'
import { QuotaPanel } from './QuotaPanel'
import { waterQuotaApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  waterQuotaApi: { getUsage: vi.fn(), createQuota: vi.fn(), deleteQuota: vi.fn() },
}))

vi.mock('../../hooks/useFarms', () => ({
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'North Farm' }] }),
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'South Zone' }] }),
}))

vi.mock('../charts', () => ({ WsBulletChart: () => <div data-testid="bullet-chart" /> }))

const USAGE = {
  quota_id: 'q1',
  target_type: 'farm',
  target_id: 'farm-1',
  target_name: 'North Farm',
  quota_liters: 1000,
  used_liters: 400,
  remaining_liters: 600,
  usage_percentage: 40,
  status: 'ok',
}

const notify = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  waterQuotaApi.getUsage.mockResolvedValue([USAGE])
})

describe('QuotaPanel', () => {
  it('shows the usage of a quota with what is left', async () => {
    render(<QuotaPanel notify={notify} />)

    await waitFor(() => expect(screen.getByText('Under quota')).toBeTruthy())
    expect(screen.getByText('40% of quota used — 600 L left')).toBeTruthy()
  })

  it('refuses a quota without target, then saves a valid one and reloads', async () => {
    waterQuotaApi.createQuota.mockResolvedValue({ id: 'q2' })
    render(<QuotaPanel notify={notify} />)
    await screen.findByText('Under quota')

    fireEvent.click(screen.getByRole('button', { name: 'Set quota' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Choose a farm.')
    expect(waterQuotaApi.createQuota).not.toHaveBeenCalled()

    fireEvent.change(screen.getByRole('combobox', { name: 'Quota target' }), { target: { value: 'farm-1' } })
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Monthly quota (L)' }), { target: { value: '1200' } })
    fireEvent.click(screen.getByRole('button', { name: 'Set quota' }))

    await waitFor(() =>
      expect(waterQuotaApi.createQuota).toHaveBeenCalledWith(
        expect.objectContaining({ targetType: 'farm', targetId: 'farm-1', quotaLiters: 1200 }),
      ),
    )
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Quota saved.'))
    await waitFor(() => expect(waterQuotaApi.getUsage).toHaveBeenCalledTimes(2))
  })

  it('deletes a quota and reloads the list', async () => {
    waterQuotaApi.deleteQuota.mockResolvedValue(undefined)
    render(<QuotaPanel notify={notify} />)
    await screen.findByText('Under quota')

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(waterQuotaApi.deleteQuota).toHaveBeenCalledWith('q1'))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Quota deleted.'))
    await waitFor(() => expect(waterQuotaApi.getUsage).toHaveBeenCalledTimes(2))
  })
})
