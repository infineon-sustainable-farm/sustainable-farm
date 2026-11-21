// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import Select from './Select'

describe('Select', () => {
  it('renders the options given as data', () => {
    render(
      <Select
        aria-label="Category"
        options={[
          { value: 'TRACTOR', label: 'Tractor' },
          { value: 'PUMP', label: 'Pump' },
        ]}
      />,
    )
    const select = screen.getByRole('combobox', { name: 'Category' })
    expect(select).toHaveClass('border-border-input', 'rounded-control')
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['Tractor', 'Pump'])
  })

  it('accepts option children and reports changes', () => {
    const onChange = vi.fn()
    render(
      <Select aria-label="Stage" defaultValue="a" onChange={onChange}>
        <option value="a">A</option>
        <option value="b">B</option>
      </Select>,
    )
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'b' } })
    expect(onChange).toHaveBeenCalled()
    expect(screen.getByRole('combobox')).toHaveValue('b')
  })
})
