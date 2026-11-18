// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { DroughtView } from './DroughtView'

vi.mock('../charts', () => ({
  WsRadialGauge: ({ value }) => <div data-testid="gauge">{value}</div>,
}))

vi.mock('../../hooks/useDashboard', () => ({
  useDroughtPrediction: () => ({
    data: {
      risk_level: 'HIGH',
      reservoir_level_percentage: 35,
      days_of_reserve_remaining: 12.34,
      daily_average_consumption_liters_7d: 800,
      total_reserve_liters: 9000,
      total_capacity_liters: 20000,
      confidence: 0.8,
    },
    loading: false,
  }),
}))

describe('DroughtView', () => {
  it('shows the risk level, the reserve figures and the current step', () => {
    render(<DroughtView notify={vi.fn()} />)

    expect(screen.getByTestId('gauge').textContent).toBe('35')
    expect(screen.getByText('12.3 j')).toBeTruthy()
    expect(screen.getByText('800 L/j')).toBeTruthy()
    expect(screen.getByText('9000 / 20000 L')).toBeTruthy()
    expect(screen.getByText('Reserve water for priority crops.')).toBeTruthy()
    const current = screen.getByRole('listitem', { current: 'step' })
    expect(current.textContent).toBe('Alert')
  })

  it('prepares the postponement plan', () => {
    const notify = vi.fn()
    render(<DroughtView notify={notify} />)

    fireEvent.click(screen.getByRole('button', { name: 'Prepare postponement plan' }))

    expect(notify).toHaveBeenCalledWith('Postponement plan prepared (demo).')
  })
})
