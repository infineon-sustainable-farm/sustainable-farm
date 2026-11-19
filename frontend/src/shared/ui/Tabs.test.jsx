// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import Tabs from './Tabs'

const tabs = [
  { id: 'solar', label: 'Solar', content: 'Solar panel' },
  { id: 'battery', label: 'Battery', content: 'Battery panel' },
  { id: 'grid', label: 'Grid', content: 'Grid panel', disabled: true },
  { id: 'meter', label: 'Meter', content: 'Meter panel' },
]

describe('Tabs', () => {
  it('renders a named tab list with one tab stop and the selected panel', () => {
    render(<Tabs tabs={tabs} label="Energy sources" />)
    expect(screen.getByRole('tablist', { name: 'Energy sources' })).toBeInTheDocument()
    const solar = screen.getByRole('tab', { name: 'Solar' })
    expect(solar).toHaveAttribute('aria-selected', 'true')
    expect(solar).toHaveAttribute('tabindex', '0')
    expect(screen.getByRole('tab', { name: 'Battery' })).toHaveAttribute('tabindex', '-1')
    const panel = screen.getByRole('tabpanel', { name: 'Solar' })
    expect(panel).toHaveTextContent('Solar panel')
    expect(solar).toHaveAttribute('aria-controls', panel.id)
  })

  it('selects on click', () => {
    const onChange = vi.fn()
    render(<Tabs tabs={tabs} label="Energy sources" onChange={onChange} />)
    fireEvent.click(screen.getByRole('tab', { name: 'Battery' }))
    expect(onChange).toHaveBeenCalledWith('battery')
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Battery panel')
  })

  it('moves with the arrows, Home and End, skipping disabled tabs', () => {
    render(<Tabs tabs={tabs} label="Energy sources" />)
    const solar = screen.getByRole('tab', { name: 'Solar' })
    solar.focus()
    fireEvent.keyDown(solar, { key: 'ArrowRight' })
    expect(screen.getByRole('tab', { name: 'Battery' })).toHaveFocus()
    fireEvent.keyDown(document.activeElement, { key: 'ArrowRight' })
    expect(screen.getByRole('tab', { name: 'Meter' })).toHaveFocus()
    expect(screen.getByRole('tab', { name: 'Meter' })).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(document.activeElement, { key: 'ArrowRight' })
    expect(solar).toHaveFocus()
    fireEvent.keyDown(solar, { key: 'ArrowLeft' })
    expect(screen.getByRole('tab', { name: 'Meter' })).toHaveFocus()
    fireEvent.keyDown(document.activeElement, { key: 'Home' })
    expect(solar).toHaveFocus()
    fireEvent.keyDown(solar, { key: 'End' })
    expect(screen.getByRole('tab', { name: 'Meter' })).toHaveFocus()
  })

  it('can be controlled', () => {
    const onChange = vi.fn()
    render(<Tabs tabs={tabs} label="Energy sources" value="battery" onChange={onChange} />)
    expect(screen.getByRole('tab', { name: 'Battery' })).toHaveAttribute('aria-selected', 'true')
    fireEvent.click(screen.getByRole('tab', { name: 'Solar' }))
    expect(onChange).toHaveBeenCalledWith('solar')
    expect(screen.getByRole('tab', { name: 'Battery' })).toHaveAttribute('aria-selected', 'true')
  })

  it('marks the selected tab by weight and surface, not by colour only', () => {
    render(<Tabs tabs={tabs} label="Energy sources" />)
    expect(screen.getByRole('tab', { name: 'Solar' })).toHaveClass('font-semibold', 'bg-surface-raised', 'shadow-raised')
  })

  it('keeps each tab label on one line and wraps the tabs instead', () => {
    render(<Tabs tabs={tabs} label="Energy sources" />)
    expect(screen.getByRole('tablist')).toHaveClass('flex-wrap')
    for (const tab of screen.getAllByRole('tab')) expect(tab).toHaveClass('whitespace-nowrap')
  })
})
