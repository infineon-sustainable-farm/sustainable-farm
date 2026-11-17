// @vitest-environment jsdom
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { Pencil } from 'lucide-react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import IconButton from './IconButton'
import Table from './Table'

const columns = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'hours', header: 'Hours', sortable: true, align: 'right' },
  { key: 'stage', header: 'Stage', cell: (row) => row.stage.toUpperCase() },
]

const rows = [
  { id: 1, name: 'Tractor 10', hours: 5, stage: 'field' },
  { id: 2, name: 'Pump', hours: 40, stage: 'water' },
  { id: 3, name: 'Tractor 2', hours: 12, stage: 'field' },
]

function bodyNames() {
  const [, body] = screen.getAllByRole('rowgroup')
  return within(body)
    .getAllByRole('row')
    .map((row) => within(row).getAllByRole('cell')[0].textContent)
}

describe('Table', () => {
  it('renders headers, cells, a caption and the machinery row look', () => {
    render(<Table columns={columns} rows={rows} caption="Equipment" />)
    expect(screen.getByRole('table', { name: 'Equipment' })).toBeInTheDocument()
    expect(screen.getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['Name', 'Hours', 'Stage'])
    expect(screen.getByText('WATER')).toBeInTheDocument()
    expect(screen.getAllByRole('row')[0]).toHaveClass('border-border-divider', 'text-text-muted')
    expect(screen.getAllByRole('row')[1]).toHaveClass('border-border-subtle')
    expect(screen.getByRole('table').parentElement).toHaveClass('overflow-x-auto')
  })

  it('sorts locally with aria-sort on the header, ascending then descending', () => {
    render(<Table columns={columns} rows={rows} />)
    const nameHeader = screen.getByRole('columnheader', { name: 'Name' })
    expect(nameHeader).toHaveAttribute('aria-sort', 'none')
    expect(screen.getByRole('columnheader', { name: 'Stage' })).not.toHaveAttribute('aria-sort')

    fireEvent.click(within(nameHeader).getByRole('button', { name: 'Name' }))
    expect(nameHeader).toHaveAttribute('aria-sort', 'ascending')
    expect(bodyNames()).toEqual(['Pump', 'Tractor 2', 'Tractor 10'])

    fireEvent.click(within(nameHeader).getByRole('button'))
    expect(nameHeader).toHaveAttribute('aria-sort', 'descending')
    expect(bodyNames()).toEqual(['Tractor 10', 'Tractor 2', 'Pump'])
  })

  it('sorts numbers as numbers', () => {
    render(<Table columns={columns} rows={rows} defaultSort={{ key: 'hours', direction: 'asc' }} />)
    expect(bodyNames()).toEqual(['Tractor 10', 'Tractor 2', 'Pump'])
  })

  it('leaves the order to the caller when the sort is controlled', () => {
    const onSortChange = vi.fn()
    render(<Table columns={columns} rows={rows} sort={{ key: 'name', direction: 'asc' }} onSortChange={onSortChange} />)
    expect(bodyNames()).toEqual(['Tractor 10', 'Pump', 'Tractor 2'])
    fireEvent.click(screen.getByRole('button', { name: 'Name' }))
    expect(onSortChange).toHaveBeenCalledWith({ key: 'name', direction: 'desc' })
  })

  it('renders row actions in a last column', () => {
    const onEdit = vi.fn()
    render(
      <Table
        columns={columns}
        rows={rows}
        rowActions={(row) => <IconButton icon={Pencil} label={`Edit ${row.name}`} onClick={() => onEdit(row)} />}
      />,
    )
    expect(screen.getByRole('columnheader', { name: 'Actions' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Edit Pump' }))
    expect(onEdit).toHaveBeenCalledWith(rows[1])
  })

  it('shows one row with the empty message', () => {
    render(<Table columns={columns} rows={[]} />)
    const cell = screen.getByRole('cell', { name: 'No results' })
    expect(cell).toHaveAttribute('colspan', '3')
    render(<Table columns={columns} rows={[]} emptyMessage="No equipment yet" />)
    expect(screen.getByText('No equipment yet')).toBeInTheDocument()
  })

  it('shows skeleton rows while loading and says so', () => {
    render(<Table columns={columns} rows={rows} loading skeletonRows={3} />)
    expect(screen.getByRole('table')).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
    expect(screen.queryByText('Pump')).not.toBeInTheDocument()
    const [, body] = screen.getAllByRole('rowgroup', { hidden: true })
    expect(body.querySelectorAll('tr')).toHaveLength(3)
  })

  it('translates its default texts', async () => {
    await act(() => i18n.changeLanguage('fr'))
    render(<Table columns={columns} rows={[]} rowActions={() => null} />)
    expect(screen.getByRole('columnheader', { name: 'Actions' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Aucun résultat' })).toBeInTheDocument()
  })
})
