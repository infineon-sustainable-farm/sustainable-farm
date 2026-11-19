// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { TriangleAlert } from 'lucide-react'
import { describe, expect, it } from 'vitest'
import Badge from './Badge'

describe('Badge', () => {
  it('shows its text with the machinery pill look', () => {
    render(<Badge tone="warning">Due in 3d</Badge>)
    expect(screen.getByText('Due in 3d')).toHaveClass(
      'rounded-pill',
      'text-xs',
      'font-semibold',
      'px-2',
      'py-0.5',
      'bg-warning-bg',
      'text-warning-fg',
    )
  })

  it('is neutral by default', () => {
    render(<Badge>In 12d</Badge>)
    expect(screen.getByText('In 12d')).toHaveClass('bg-neutral-bg', 'text-neutral-fg')
  })

  it('renders an icon next to the text, hidden from assistive technologies', () => {
    render(
      <Badge tone="warning" icon={TriangleAlert}>
        Low stock
      </Badge>,
    )
    expect(screen.getByText('Low stock').querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('names an icon-only badge with its label', () => {
    render(<Badge tone="error" icon={TriangleAlert} label="Overdue" />)
    expect(screen.getByRole('img', { name: 'Overdue' })).toBeInTheDocument()
  })
})
