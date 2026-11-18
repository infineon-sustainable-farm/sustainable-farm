// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { DashboardView } from './DashboardView'

vi.mock('../../hooks/useDashboard', () => ({
  useKpis: () => ({ data: { daily_consumption_liters: 1200, tank_level_percentage: 64, anomaly_count: 2, water_quality_status: 'GOOD' }, loading: false }),
  useAlerts: () => ({ data: [{ type: 'warning', title: 'Leak suspected', message: 'Zone B flow' }], loading: false }),
  useActivities: () => ({ data: [], loading: false }),
  useRecommendations: () => ({ data: [{ priority: 'high', recommendation: 'Water at dawn' }], loading: false }),
  useDroughtPrediction: () => ({ data: { risk_level: 'MEDIUM', recommendations: 'Watch the reserve' }, loading: false }),
  useWeather: () => ({ data: null, loading: false }),
  useHealthCheck: () => ({ healthy: true, loading: false }),
  useWaterBalance: () => ({ data: { balance_status: 'deficit', available_after_forecast_liters: -50, demand_forecast_liters: 300 }, loading: false }),
}))

describe('DashboardView', () => {
  it('shows the KPIs, the panels and the backend status', () => {
    render(<DashboardView onNavigate={vi.fn()} notify={vi.fn()} />)

    expect(screen.getByText('Backend connected')).toBeTruthy()
    expect(screen.getByText('1200 L')).toBeTruthy()
    expect(screen.getByText('64%')).toBeTruthy()
    expect(screen.getByText('GOOD')).toBeTruthy()
    expect(screen.getByText('Leak suspected')).toBeTruthy()
    expect(screen.getByText('Water at dawn')).toBeTruthy()
    expect(screen.getByText('Deficit')).toBeTruthy()
    expect(screen.getByText('300 L planned, -50 L left')).toBeTruthy()
    expect(screen.getByText('No activity')).toBeTruthy()
  })

  it('opens other screens and shows the demo notices from the quick actions', () => {
    const onNavigate = vi.fn()
    const notify = vi.fn()
    render(<DashboardView onNavigate={onNavigate} notify={notify} />)

    fireEvent.click(screen.getByRole('button', { name: 'Manage irrigation' }))
    fireEvent.click(screen.getByRole('button', { name: 'Plan an outage' }))
    fireEvent.click(screen.getByRole('button', { name: 'Prepared carry-over plan' }))

    expect(onNavigate).toHaveBeenCalledWith('irrigation')
    expect(onNavigate).toHaveBeenCalledWith('outages')
    expect(notify).toHaveBeenCalledWith('Demo: report prepared.')
  })
})
