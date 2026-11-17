// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { Wrench } from 'lucide-react'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge'

describe('StatusBadge', () => {
  it('shows the state as text with the machinery StatusBadge look', () => {
    const { container } = render(<StatusBadge tone="success" label="Operational" />)
    expect(screen.getByText('Operational')).toHaveClass('py-1', 'px-2', 'rounded-pill', 'bg-success-bg', 'text-success-fg')
    expect(container.querySelector('svg')).toBeNull()
  })

  it.each(['success', 'warning', 'error', 'info', 'neutral'])('adds the icon of the %s tone on request', (tone) => {
    render(<StatusBadge tone={tone} label="State" withIcon />)
    expect(screen.getByText('State').querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('accepts its own icon', () => {
    render(<StatusBadge tone="warning" label="Under maintenance" icon={Wrench} />)
    expect(screen.getByText('Under maintenance').querySelector('svg')).toBeInTheDocument()
  })
})
