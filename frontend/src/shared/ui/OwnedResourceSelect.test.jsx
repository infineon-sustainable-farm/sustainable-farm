// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import { OwnedResourceSelect, OwnerHint } from '.'

const OWNER = { moduleId: 'machinery', createPath: '/machinery/equipment-registry?create=1' }
const MACHINES = [
  { id: 1, name: 'Dryer A' },
  { id: 2, name: 'Washer B' },
]

function renderSelect(props) {
  return render(
    <MemoryRouter>
      <OwnedResourceSelect label="Machine" owner={OWNER} {...props} />
    </MemoryRouter>,
  )
}

describe('OwnedResourceSelect', () => {
  it('lists the loaded options and returns the chosen id with its option', () => {
    const onChange = vi.fn()
    renderSelect({ options: MACHINES, value: '', onChange })

    const select = screen.getByLabelText('Machine')
    expect(screen.getByRole('option', { name: 'Choose…' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Dryer A' })).toHaveValue('1')
    fireEvent.change(select, { target: { value: '2' } })
    expect(onChange).toHaveBeenCalledWith('2', MACHINES[1])
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('reads labels and values through the getters', () => {
    renderSelect({
      options: [{ code: 'KEITT', label: 'Keitt' }],
      getOptionValue: (option) => option.code,
      getOptionLabel: (option) => option.label,
      placeholder: 'Pick a variety',
      value: 'KEITT',
    })
    expect(screen.getByLabelText('Machine')).toHaveValue('KEITT')
    expect(screen.getByRole('option', { name: 'Pick a variety' })).toBeInTheDocument()
  })

  it('sends the user to the owner module when there is nothing to choose', () => {
    renderSelect({ options: [] })

    const select = screen.getByLabelText('Machine')
    expect(select).toBeDisabled()
    expect(screen.getByRole('option', { name: 'None available' })).toBeInTheDocument()
    const hint = screen.getByText('Ask the Machinery module to add it.').closest('p')
    expect(select.getAttribute('aria-describedby').split(' ')).toContain(hint.id)
    expect(screen.getByRole('link', { name: 'Add it in Machinery' })).toHaveAttribute(
      'href',
      '/machinery/equipment-registry?create=1',
    )
  })

  it('keeps a discreet link under a filled list when asked', () => {
    renderSelect({ options: MACHINES, showOwnerLinkAlways: true })
    expect(screen.getByText('Not in the list? Ask the Machinery module.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Add it in Machinery' })).toBeInTheDocument()
  })

  it('waits while the list loads, without the owner hint', () => {
    renderSelect({ loading: true })
    const select = screen.getByLabelText('Machine')
    expect(select).toBeDisabled()
    expect(select).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByRole('option', { name: 'Loading…' })).toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('shows a load error with a retry button', () => {
    const onRetry = vi.fn()
    renderSelect({ loadError: true, onRetry })

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('The list could not load.')
    expect(screen.getByLabelText('Machine').getAttribute('aria-describedby').split(' ')).toContain(alert.id)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it('shows the message of the load error it is given', () => {
    renderSelect({ loadError: 'Machinery is unreachable.' })
    expect(screen.getByRole('alert')).toHaveTextContent('Machinery is unreachable.')
  })

  it('carries the field hint, the validation error and required', () => {
    renderSelect({ options: MACHINES, hint: 'Used for the run', error: 'Choose a machine.', required: true })
    const select = screen.getByLabelText('Machine')
    expect(select).toBeRequired()
    expect(select).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Choose a machine.')).toBeInTheDocument()
  })

  it('works without an owner', () => {
    render(<OwnedResourceSelect label="Machine" options={[]} />)
    expect(screen.getByLabelText('Machine')).toBeDisabled()
    expect(screen.queryByText(/Ask the/)).not.toBeInTheDocument()
  })
})

describe('OwnerHint', () => {
  it('names the owner module in the current language and links to its create form', () => {
    render(
      <MemoryRouter>
        <OwnerHint owner={{ moduleId: 'watersupply', createPath: '/watersupply/farms?create=zone' }} />
      </MemoryRouter>,
    )
    expect(screen.getByText('Ask the Water supply module to add it.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Add it in Water supply' })).toHaveAttribute(
      'href',
      '/watersupply/farms?create=zone',
    )
  })

  it('is translated, the module name included', async () => {
    await i18n.changeLanguage('fr')
    await i18n.loadNamespaces(['common', 'machinery'])
    render(
      <MemoryRouter>
        <OwnerHint owner={OWNER} />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Demandez au module Machines de l’ajouter.')).toBeInTheDocument()
  })
})
