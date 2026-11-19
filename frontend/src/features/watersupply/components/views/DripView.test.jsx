// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { DripView } from './DripView'
import { dripMaintenanceApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  dripMaintenanceApi: { getLogs: vi.fn(), createLog: vi.fn(), updateLog: vi.fn(), deleteLog: vi.fn() },
}))

vi.mock('../../../../shared/api/persons', () => ({
  fetchPersons: vi.fn(() => Promise.resolve([{ id: 4, fullName: 'Issa Technician' }])),
}))

vi.mock('../../hooks/useFarms', () => ({
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'Zone A' }] }),
}))

const LOG = {
  id: 'log-1',
  zoneId: 'zone-1',
  maintenanceDate: '2026-09-01T08:00:00Z',
  maintenanceType: 'cleaning',
  cloggingDetected: true,
  cloggingSeverity: 'high',
  emitterReplacedCount: 3,
}

const notify = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  dripMaintenanceApi.getLogs.mockResolvedValue([LOG])
})

describe('DripView', () => {
  it('lists the interventions with their state', async () => {
    render(<DripView notify={notify} />)

    await waitFor(() => expect(screen.getByText('Clogging high')).toBeTruthy())
    expect(screen.getByText('2026-09-01 08:00')).toBeTruthy()
    expect(screen.getByText('1 intervention(s) in the last 7 days (relative to the latest data).', { exact: false })).toBeTruthy()
  })

  it('validates the zone before saving an intervention', async () => {
    render(<DripView notify={notify} />)
    await screen.findByText('Clogging high')

    fireEvent.submit(document.querySelector('form'))

    expect(screen.getByRole('alert')).toHaveTextContent('The zone is required.')
    expect(dripMaintenanceApi.createLog).not.toHaveBeenCalled()
  })

  it('records who performed an intervention as a person of the directory', async () => {
    dripMaintenanceApi.updateLog.mockResolvedValue({})
    const { container } = render(<DripView notify={notify} />)
    await screen.findByText('Clogging high')

    fireEvent.click(screen.getByRole('button', { name: 'Edit' }))
    await screen.findByRole('option', { name: 'Issa Technician' })
    fireEvent.change(controlByLabel(container, 'Performed by'), { target: { value: '4' } })
    fireEvent.submit(document.querySelector('form'))

    await waitFor(() => expect(dripMaintenanceApi.updateLog).toHaveBeenCalledWith(
      'log-1', expect.objectContaining({ performedByPersonId: 4 })))
    expect(dripMaintenanceApi.updateLog.mock.calls[0][1]).not.toHaveProperty('performedBy')
  })

  it('deletes an intervention after confirmation', async () => {
    dripMaintenanceApi.deleteLog.mockResolvedValue(undefined)
    render(<DripView notify={notify} />)
    await screen.findByText('Clogging high')

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('This intervention will be removed from the maintenance history.')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(dripMaintenanceApi.deleteLog).toHaveBeenCalledWith('log-1'))
    await waitFor(() => expect(notify).toHaveBeenCalledWith('Intervention deleted'))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('keeps the dialog open with the error when the deletion fails', async () => {
    dripMaintenanceApi.deleteLog.mockRejectedValue(new Error('Server down'))
    render(<DripView notify={notify} />)
    await screen.findByText('Clogging high')

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(within(dialog).getByRole('alert')).toHaveTextContent('Server down'))
    expect(notify).not.toHaveBeenCalled()
  })
})
