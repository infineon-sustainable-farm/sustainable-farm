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
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'Ferme Nord' }] }),
}))

beforeEach(() => {
  vi.clearAllMocks()
  waterSourceApi.getSources.mockResolvedValue([
    { id: 'src-1', farmId: 'farm-1', name: 'Forage principal', type: 'borehole', capacityLiters: 5000, currentLevelLiters: 2500 },
  ])
})

const props = { notify: vi.fn() }

describe('SourcesView', () => {
  it('affiche la liste des sources avec leur ferme', async () => {
    render(<SourcesView {...props} />)

    await waitFor(() => expect(screen.getByText('Forage principal')).toBeTruthy())
    expect(screen.getAllByText('Ferme Nord').length).toBeGreaterThan(0)
    expect(screen.getByText(/2500 L \(50%\)/)).toBeTruthy()
  })

  it('affiche un état vide', async () => {
    waterSourceApi.getSources.mockResolvedValue([])

    render(<SourcesView {...props} />)

    await waitFor(() => expect(screen.getByText('Aucune source')).toBeTruthy())
  })

  it('valide les champs obligatoires avant création', async () => {
    render(<SourcesView {...props} />)
    await screen.findByText('Forage principal')

    fireEvent.submit(document.querySelector('form'))

    await waitFor(() => expect(screen.getByText('La ferme est obligatoire.')).toBeTruthy())
    expect(waterSourceApi.createSource).not.toHaveBeenCalled()
  })

  it('crée une source associée à une ferme puis rafraîchit', async () => {
    waterSourceApi.createSource.mockResolvedValue({ id: 'src-2' })

    render(<SourcesView {...props} />)
    await screen.findByText('Forage principal')

    const form = document.querySelector('form')
    const selects = form.querySelectorAll('select')
    fireEvent.change(selects[0], { target: { value: 'farm-1' } }) // ferme
    fireEvent.change(selects[1], { target: { value: 'river' } }) // type
    const textInput = form.querySelector('input:not([type="number"])')
    fireEvent.change(textInput, { target: { value: 'Rivière Est' } })
    const numbers = form.querySelectorAll('input[type="number"]')
    fireEvent.change(numbers[0], { target: { value: '3000' } }) // capacité
    fireEvent.submit(form)

    await waitFor(() =>
      expect(waterSourceApi.createSource).toHaveBeenCalledWith({
        farmId: 'farm-1',
        name: 'Rivière Est',
        type: 'river',
        capacityLiters: 3000,
        currentLevelLiters: 0,
      }),
    )
    await waitFor(() => expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2))
  })

  it('supprime une source après confirmation', async () => {
    waterSourceApi.deleteSource.mockResolvedValue(undefined)

    render(<SourcesView {...props} />)
    await screen.findByText('Forage principal')

    fireEvent.click(screen.getByTitle('Supprimer'))
    await waitFor(() => expect(screen.getByRole('dialog')).toBeTruthy())

    fireEvent.click(screen.getByText('Supprimer', { selector: 'div[role="dialog"] button' }))
    await waitFor(() => expect(waterSourceApi.deleteSource).toHaveBeenCalledWith('src-1'))
    await waitFor(() => expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2))
  })
})