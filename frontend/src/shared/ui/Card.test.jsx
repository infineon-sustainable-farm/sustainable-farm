// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Card from './Card'

describe('Card', () => {
  it('renders a section with the machinery card look', () => {
    const { container } = render(<Card>Body</Card>)
    const card = container.firstChild
    expect(card.tagName).toBe('SECTION')
    expect(card).toHaveClass('rounded-card', 'border-border-subtle', 'bg-surface-raised', 'shadow-raised', 'p-5')
    expect(screen.getByText('Body')).toBeInTheDocument()
  })

  it('renders a title heading, a subtitle and actions', () => {
    render(
      <Card title="Fuel" subtitle="Last 6 months" actions={<button type="button">Export</button>} titleAs="h3">
        Chart
      </Card>,
    )
    expect(screen.getByRole('heading', { level: 3, name: 'Fuel' })).toBeInTheDocument()
    expect(screen.getByText('Last 6 months')).toHaveClass('text-text-muted')
    expect(screen.getByRole('button', { name: 'Export' })).toBeInTheDocument()
  })

  it('can drop its padding for a table that fills it', () => {
    const { container } = render(<Card padding="none">Table</Card>)
    expect(container.firstChild).not.toHaveClass('p-5')
  })
})
