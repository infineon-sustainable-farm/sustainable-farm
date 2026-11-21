// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { Cog } from 'lucide-react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import KpiCard from './KpiCard'

describe('KpiCard', () => {
  it('shows value, label and hint', () => {
    render(<KpiCard icon={Cog} label="Total assets" value={12} hint="3 under maintenance" />)
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('Total assets')).toBeInTheDocument()
    expect(screen.getByText('3 under maintenance')).toHaveClass('text-text-muted')
  })

  it('is a focusable link when it has a route', () => {
    render(
      <MemoryRouter>
        <KpiCard icon={Cog} label="Total assets" value={12} to="/machinery/equipment-registry" />
      </MemoryRouter>,
    )
    const link = screen.getByRole('link', { name: /total assets/i })
    expect(link).toHaveAttribute('href', '/machinery/equipment-registry')
    expect(link.className).toContain('focus-visible:outline-2')
    expect(link.className).not.toContain('hover:scale')
  })

  it.each([
    ['primary', 'bg-primary-tint', 'text-primary'],
    ['warning', 'bg-warning-tint', 'text-warning-fg'],
    ['error', 'bg-error-tint', 'text-error-fg'],
    ['info', 'bg-info-tint', 'text-info-fg'],
  ])('tints the icon tile for the %s tone', (tone, bg, fg) => {
    const { container } = render(<KpiCard icon={Cog} label="L" value={1} tone={tone} />)
    const tile = container.querySelector('span')
    expect(tile).toHaveClass(bg, fg, 'rounded-tile', 'h-10', 'w-10')
    expect(tile.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })
})
