import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { IrrigationView } from './IrrigationView'
import { irrigationApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  irrigationApi: {
    getSchedules: vi.fn(),
    getLogs: vi.fn(),
    createSchedule: vi.fn(),
    updateSchedule: vi.fn(),
    deleteSchedule: vi.fn(),
    createLog: vi.fn(),
    updateLog: vi.fn(),
    deleteLog: vi.fn(),
    startIrrigation: vi.fn(),
    stopIrrigation: vi.fn(),
  },
}))

vi.mock('../../hooks/useFarms', () => ({
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'Zone A', fieldId: 'f1', areaHectares: 2 }] }),
}))

const SCHEDULES = [
  { id: 'sch-1', zoneId: 'zone-1', startTime: '2026-09-01T06:00:00Z', durationMinutes: 45, waterQuantityLiters: 1200, status: 'scheduled' },
  { id: 'sch-2', zoneId: 'zone-1', startTime: '2026-09-01T08:00:00Z', durationMinutes: 30, waterQuantityLiters: 800, status: 'running' },
  // Completed schedule: archived in the journal, it must no longer appear in the active schedules.
  { id: 'sch-3', zoneId: 'zone-1', startTime: '2026-08-30T06:00:00Z', durationMinutes: 20, waterQuantityLiters: 900, status: 'completed' },
]

const LOGS = [
  // Completed cycle: visible in the journal.
  { id: 'log-1', scheduleId: 'sch-3', actualStartTime: '2026-08-30T06:00:00Z', actualEndTime: '2026-08-30T06:20:00Z', waterUsedLiters: 300, status: 'completed' },
  // Cycle still running: it must NOT appear in the journal.
  { id: 'log-2', scheduleId: 'sch-2', actualStartTime: '2026-09-01T08:00:00Z', actualEndTime: null, waterUsedLiters: 120, status: 'in_progress' },
]

function setupApi() {
  irrigationApi.getSchedules.mockImplementation(() => Promise.resolve([...SCHEDULES]))
  irrigationApi.getLogs.mockImplementation(() => Promise.resolve([...LOGS]))
}

beforeEach(() => {
  vi.clearAllMocks()
  setupApi()
})

const props = { notify: vi.fn() }

describe('IrrigationView', () => {
  it('displays the active schedules and the completed journal cycles', async () => {
    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('Zone A')).toBeTruthy())
    expect(screen.getByText(/45 min/)).toBeTruthy()

    // Journal: the completed cycle is listed with its actual volume.
    expect(screen.getByText('300 L')).toBeTruthy()
    // The cycle still running does not appear in the journal.
    expect(screen.queryByText('120 L')).toBeNull()
    // The completed schedule is archived: absent from the active schedules panel.
    expect(screen.queryByText('900 L planned')).toBeNull()
  })

  it('displays an empty state when there is no data', async () => {
    irrigationApi.getSchedules.mockResolvedValue([])
    irrigationApi.getLogs.mockResolvedValue([])

    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('No running or scheduled plans')).toBeTruthy())
    expect(screen.getByText('No completed cycles')).toBeTruthy()
  })

  it('limits the journal to the last 10 completed cycles (the rest stays in the database)', async () => {
    const many = Array.from({ length: 12 }, (_, i) => {
      const day = String(20 - i).padStart(2, '0')
      return {
        id: `log-${i}`,
        scheduleId: 'sch-3',
        actualStartTime: `2026-08-${day}T06:00:00Z`,
        actualEndTime: `2026-08-${day}T06:30:00Z`,
        waterUsedLiters: 100 + i,
        status: 'completed',
      }
    })
    irrigationApi.getLogs.mockResolvedValue(many)

    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('12 completed cycle(s)')).toBeTruthy())
    // Only the 10 most recent are rendered (volumes 100 L to 109 L).
    expect(screen.getAllByText(/^1\d\d L$/).length).toBe(10)
    // The 11th stays in the database: not displayed, but reported to the user.
    expect(screen.queryByText('110 L')).toBeNull()
    expect(screen.getByText(/Older cycles remain stored/)).toBeTruthy()
  })

  it('displays a usable error when the API fails', async () => {
    irrigationApi.getSchedules.mockRejectedValue(new Error('Backend unreachable'))

    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('Error')).toBeTruthy())
    expect(screen.getByText(/Backend unreachable/)).toBeTruthy()
  })

  it('starts a scheduled irrigation and refreshes', async () => {
    irrigationApi.startIrrigation.mockResolvedValue({ id: 'log-new' })

    render(<IrrigationView {...props} />)
    const startButtons = await screen.findAllByTitle('Start now')
    fireEvent.click(startButtons[0])

    await waitFor(() => expect(irrigationApi.startIrrigation).toHaveBeenCalledWith('sch-1'))
    // refetch: getSchedules called again after the action
    await waitFor(() => expect(irrigationApi.getSchedules).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(props.notify).toHaveBeenCalledWith(expect.stringContaining('started')))
  })

  it('stops a running irrigation', async () => {
    irrigationApi.stopIrrigation.mockResolvedValue({ id: 'log-closed' })

    render(<IrrigationView {...props} />)
    const stopButtons = await screen.findAllByTitle('Stop irrigation')
    fireEvent.click(stopButtons[0])

    await waitFor(() => expect(irrigationApi.stopIrrigation).toHaveBeenCalledWith('sch-2'))
    await waitFor(() => expect(irrigationApi.getLogs).toHaveBeenCalledTimes(2))
  })

  it('creates a schedule manually and refreshes', async () => {
    irrigationApi.createSchedule.mockResolvedValue({ id: 'sch-new' })

    render(<IrrigationView {...props} />)
    await screen.findByText('Zone A')

    const scheduleForm = document.querySelector('form')

    const selects = scheduleForm.querySelectorAll('select')
    fireEvent.change(selects[0], { target: { value: 'zone-1' } })
    const inputs = scheduleForm.querySelectorAll('input')
    fireEvent.change(inputs[0], { target: { value: '2026-09-05T06:00' } }) // datetime-local
    fireEvent.change(inputs[1], { target: { value: '30' } }) // duration
    fireEvent.change(inputs[2], { target: { value: '500' } }) // volume
    fireEvent.submit(scheduleForm)

    await waitFor(() =>
      expect(irrigationApi.createSchedule).toHaveBeenCalledWith(
        expect.objectContaining({ zoneId: 'zone-1', durationMinutes: 30, waterQuantityLiters: 500 }),
      ),
    )
    await waitFor(() => expect(irrigationApi.getSchedules).toHaveBeenCalledTimes(2))
  })

  it('displays a validation error when the duration is invalid', async () => {
    render(<IrrigationView {...props} />)
    await screen.findByText('Zone A')

    const scheduleForm = document.querySelector('form')
    const selects = scheduleForm.querySelectorAll('select')
    fireEvent.change(selects[0], { target: { value: 'zone-1' } })
    const inputs = scheduleForm.querySelectorAll('input')
    fireEvent.change(inputs[0], { target: { value: '2026-09-05T06:00' } })
    fireEvent.change(inputs[1], { target: { value: '0' } })
    fireEvent.change(inputs[2], { target: { value: '500' } })
    fireEvent.submit(scheduleForm)

    await waitFor(() => expect(screen.getByText('Duration must be greater than 0.')).toBeTruthy())
    expect(irrigationApi.createSchedule).not.toHaveBeenCalled()
  })
})