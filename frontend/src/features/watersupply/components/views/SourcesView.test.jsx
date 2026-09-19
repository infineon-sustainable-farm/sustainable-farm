import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { SourcesView } from './SourcesView'
import { waterSourceApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  waterSourceApi: {
    getSources: vi.fn(),
    createSource: vi.fn(),
    updateSource: vi.fn(),
    deleteSource: vi.fn(),
  },
}))

vi.mock('../../hooks/useFarms', () => ({
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'North Farm' }] }),
}))

beforeEach(() => {
  vi.clearAllMocks()
  waterSourceApi.getSources.mockResolvedValue([
    { id: 'src-1', farmId: 'farm-1', name: 'Main borehole', type: 'borehole', capacityLiters: 5000, currentLevelLiters: 2500 },
  ])
})

const props = { notify: vi.fn() }

describe('SourcesView', () => {
  it('displays the list of sources with their farm', async () => {
    render(<SourcesView {...props} />)

    await waitFor(() => expect(screen.getByText('Main borehole')).toBeTruthy())
    expect(screen.getAllByText('North Farm').length).toBeGreaterThan(0)
    expect(screen.getByText(/2500 L \(50%\)/)).toBeTruthy()
  })

  it('displays an empty state', async () => {
    waterSourceApi.getSources.mockResolvedValue([])

    render(<SourcesView {...props} />)

    await waitFor(() => expect(screen.getByText('No sources')).toBeTruthy())
  })

  it('validates required fields before creation', async () => {
    render(<SourcesView {...props} />)
    await screen.findByText('Main borehole')

    fireEvent.submit(document.querySelector('form'))

    await waitFor(() => expect(screen.getByText('The farm is required.')).toBeTruthy())
    expect(waterSourceApi.createSource).not.toHaveBeenCalled()
  })

  it('creates a source linked to a farm then refreshes', async () => {
    waterSourceApi.createSource.mockResolvedValue({ id: 'src-2' })

    render(<SourcesView {...props} />)
    await screen.findByText('Main borehole')

    const form = document.querySelector('form')
    const selects = form.querySelectorAll('select')
    fireEvent.change(selects[0], { target: { value: 'farm-1' } }) // farm
    fireEvent.change(selects[1], { target: { value: 'river' } }) // type
    const textInput = form.querySelector('input:not([type="number"])')
    fireEvent.change(textInput, { target: { value: 'East River' } })
    const numbers = form.querySelectorAll('input[type="number"]')
    fireEvent.change(numbers[0], { target: { value: '3000' } }) // capacity
    fireEvent.submit(form)

    await waitFor(() =>
      expect(waterSourceApi.createSource).toHaveBeenCalledWith({
        farmId: 'farm-1',
        name: 'East River',
        type: 'river',
        capacityLiters: 3000,
        currentLevelLiters: 0,
      }),
    )
    await waitFor(() => expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2))
  })

  it('deletes a source after confirmation', async () => {
    waterSourceApi.deleteSource.mockResolvedValue(undefined)

    render(<SourcesView {...props} />)
    await screen.findByText('Main borehole')

    fireEvent.click(screen.getByTitle('Delete'))
    await waitFor(() => expect(screen.getByRole('dialog')).toBeTruthy())

    fireEvent.click(screen.getByText('Delete', { selector: 'div[role="dialog"] button' }))
    await waitFor(() => expect(waterSourceApi.deleteSource).toHaveBeenCalledWith('src-1'))
    await waitFor(() => expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2))
  })
})