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
  // Planning terminé : archivé dans le journal, il ne doit plus apparaître dans les plannings actifs.
  { id: 'sch-3', zoneId: 'zone-1', startTime: '2026-08-30T06:00:00Z', durationMinutes: 20, waterQuantityLiters: 900, status: 'completed' },
]

const LOGS = [
  // Cycle terminé : visible dans le journal.
  { id: 'log-1', scheduleId: 'sch-3', actualStartTime: '2026-08-30T06:00:00Z', actualEndTime: '2026-08-30T06:20:00Z', waterUsedLiters: 300, status: 'completed' },
  // Cycle encore en cours : il ne doit PAS apparaître dans le journal.
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
  it('affiche les plannings actifs et les cycles terminés du journal', async () => {
    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('Zone A')).toBeTruthy())
    expect(screen.getByText(/45 min/)).toBeTruthy()

    // Journal : le cycle terminé est listé avec son volume réel.
    expect(screen.getByText('300 L')).toBeTruthy()
    // Le cycle encore en cours ne figure pas dans le journal.
    expect(screen.queryByText('120 L')).toBeNull()
    // Le planning terminé est archivé : absent du panneau des plannings actifs.
    expect(screen.queryByText('900 L prévus')).toBeNull()
  })

  it('affiche un état vide quand aucune donnée', async () => {
    irrigationApi.getSchedules.mockResolvedValue([])
    irrigationApi.getLogs.mockResolvedValue([])

    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('Aucun planning en cours ou prévu')).toBeTruthy())
    expect(screen.getByText('Aucun cycle terminé')).toBeTruthy()
  })

  it('limite le journal aux 10 derniers cycles terminés (le reste reste en base)', async () => {
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

    await waitFor(() => expect(screen.getByText('12 cycle(s) terminé(s)')).toBeTruthy())
    // Seuls les 10 plus récents sont rendus (volumes 100 L à 109 L).
    expect(screen.getAllByText(/^1\d\d L$/).length).toBe(10)
    // Le 11e reste en base : non affiché, mais signalé à l'utilisateur.
    expect(screen.queryByText('110 L')).toBeNull()
    expect(screen.getByText(/2 cycle\(s\) plus ancien\(s\)/)).toBeTruthy()
  })

  it('affiche une erreur exploitable quand l’API échoue', async () => {
    irrigationApi.getSchedules.mockRejectedValue(new Error('Backend injoignable'))

    render(<IrrigationView {...props} />)

    await waitFor(() => expect(screen.getByText('Erreur')).toBeTruthy())
    expect(screen.getByText(/Backend injoignable/)).toBeTruthy()
  })

  it('démarre une irrigation planifiée et rafraîchit', async () => {
    irrigationApi.startIrrigation.mockResolvedValue({ id: 'log-new' })

    render(<IrrigationView {...props} />)
    const startButtons = await screen.findAllByTitle('Démarrer maintenant')
    fireEvent.click(startButtons[0])

    await waitFor(() => expect(irrigationApi.startIrrigation).toHaveBeenCalledWith('sch-1'))
    // refetch : getSchedules rappelé après l'action
    await waitFor(() => expect(irrigationApi.getSchedules).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(props.notify).toHaveBeenCalledWith(expect.stringContaining('démarrée')))
  })

  it('arrête une irrigation en cours', async () => {
    irrigationApi.stopIrrigation.mockResolvedValue({ id: 'log-closed' })

    render(<IrrigationView {...props} />)
    const stopButtons = await screen.findAllByTitle("Arrêter l'irrigation")
    fireEvent.click(stopButtons[0])

    await waitFor(() => expect(irrigationApi.stopIrrigation).toHaveBeenCalledWith('sch-2'))
    await waitFor(() => expect(irrigationApi.getLogs).toHaveBeenCalledTimes(2))
  })

  it('crée manuellement un planning et rafraîchit', async () => {
    irrigationApi.createSchedule.mockResolvedValue({ id: 'sch-new' })

    render(<IrrigationView {...props} />)
    await screen.findByText('Zone A')

    const scheduleForm = document.querySelector('form')

    const selects = scheduleForm.querySelectorAll('select')
    fireEvent.change(selects[0], { target: { value: 'zone-1' } })
    const inputs = scheduleForm.querySelectorAll('input')
    fireEvent.change(inputs[0], { target: { value: '2026-09-05T06:00' } }) // datetime-local
    fireEvent.change(inputs[1], { target: { value: '30' } }) // durée
    fireEvent.change(inputs[2], { target: { value: '500' } }) // volume
    fireEvent.submit(scheduleForm)

    await waitFor(() =>
      expect(irrigationApi.createSchedule).toHaveBeenCalledWith(
        expect.objectContaining({ zoneId: 'zone-1', durationMinutes: 30, waterQuantityLiters: 500 }),
      ),
    )
    await waitFor(() => expect(irrigationApi.getSchedules).toHaveBeenCalledTimes(2))
  })

  it('affiche une erreur de validation si la durée est invalide', async () => {
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

    await waitFor(() => expect(screen.getByText('La durée doit être supérieure à 0.')).toBeTruthy())
    expect(irrigationApi.createSchedule).not.toHaveBeenCalled()
  })
})