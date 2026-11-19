// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchPersons } from '../shared/api/persons'
import PersonSelect from './PersonSelect'

vi.mock('../shared/api/persons', () => ({ fetchPersons: vi.fn() }))

const PEOPLE = [
  { id: 1, fullName: 'Awa Traoré', active: true },
  { id: 2, fullName: 'Issa Kaboré', active: true },
]

function renderSelect(props = {}) {
  return render(
    <MemoryRouter>
      <PersonSelect label="Operator" {...props} />
    </MemoryRouter>,
  )
}

describe('PersonSelect', () => {
  beforeEach(() => {
    vi.mocked(fetchPersons).mockReset()
  })

  it('lists the people of the directory by name and returns the chosen id', async () => {
    vi.mocked(fetchPersons).mockResolvedValue(PEOPLE)
    const onChange = vi.fn()
    renderSelect({ value: '', onChange })

    expect(await screen.findByRole('option', { name: 'Issa Kaboré' })).toHaveValue('2')
    fireEvent.change(screen.getByLabelText('Operator'), { target: { value: '2' } })
    expect(onChange).toHaveBeenCalledWith('2', PEOPLE[1])
  })

  it('keeps the current person of a record when the directory no longer lists it', async () => {
    vi.mocked(fetchPersons).mockResolvedValue(PEOPLE)
    renderSelect({ value: 9, currentName: 'Former Worker' })

    expect(await screen.findByRole('option', { name: 'Former Worker' })).toHaveValue('9')
    expect(screen.getByLabelText('Operator')).toHaveValue('9')
  })

  it('sends the user to the Staff screen when the directory is empty', async () => {
    vi.mocked(fetchPersons).mockResolvedValue([])
    renderSelect()

    const link = await screen.findByRole('link')
    expect(link).toHaveAttribute('href', '/visitormanagement/staff?create=1')
  })

  it('shows the load error and retries', async () => {
    vi.mocked(fetchPersons).mockRejectedValueOnce(new Error('down')).mockResolvedValue(PEOPLE)
    renderSelect()

    fireEvent.click(await screen.findByRole('button', { name: 'Retry' }))
    await waitFor(() => expect(fetchPersons).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('option', { name: 'Awa Traoré' })).toBeInTheDocument()
  })
})
