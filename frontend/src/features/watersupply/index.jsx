/**
 * Point d'entrée du module watersupply.
 * Exporte tous les composants, hooks et API du module.
 */

// Components
export { LoginForm } from './components/LoginForm'
export { Dashboard } from './components/Dashboard'
export { WaterSupplyApp } from './components/WaterSupplyApp'

// Hooks
export { useKpis, useAlerts, useActivities, useRecommendations, useDroughtPrediction, useWeather, useHealthCheck } from './hooks/useDashboard'
export { useFarms, useFarmFields, useFieldZones, useZones } from './hooks/useFarms'
export { useWaterSources, useWaterConsumptions, useWaterQualityTests, useIrrigationSchedules, useIrrigationLogs, useNotifications, useRainwaterHarvests, useDripMaintenanceLogs } from './hooks/useWaterData'

// API
export * from './api/watersupplyApi'
