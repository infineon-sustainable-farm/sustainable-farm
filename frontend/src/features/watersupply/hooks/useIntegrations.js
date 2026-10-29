import {
  iotCommandApi,
  outageApi,
  plantsIntegrationApi,
  productTransformationApi,
  visitorApi,
  waterSourceApi,
  zoneBlockMappingApi,
} from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Data of the inter-module integrations (Plants, Visitor Management, Product Transformation,
 * IoT commands), served by React Query like the rest of the module. Lookups that need an
 * identifier stay idle until it is provided (`enabled`).
 */

const asList = (data) => (Array.isArray(data) ? data : data?.content ?? [])

export function useZoneBlockMappings() {
  const { data, loading, error, refetch } = useModuleQuery(
    ['watersupply', 'zone-block-mappings'],
    () => zoneBlockMappingApi.getMappings(),
  )
  return { mappings: asList(data), loading, error, refetch }
}

/** Rain gauge rainfall of a block over [from, to] (days, both included). */
export function useBlockRainfall(blockCode, from, to) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'block-rainfall', blockCode ?? 'none', from, to],
    () => plantsIntegrationApi.getRainfall(blockCode, from, to),
    { enabled: Boolean(blockCode) },
  )
  return { data, loading, error }
}

/**
 * Irrigation need of a block for one day. The endpoint is a POST because it takes a body, but it
 * only computes: it is read like any other query. Without `growthPhase` the backend reads the
 * phase from the Plants growth calendar.
 */
export function useBlockIrrigationNeed(blockCode, growthPhase, date) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'block-irrigation-need', blockCode ?? 'none', growthPhase || 'plants', date],
    () => plantsIntegrationApi.getIrrigationNeed({ blockCode, growthPhase: growthPhase || undefined, date }),
    { enabled: Boolean(blockCode) },
  )
  return { data, loading, error }
}

export function useOutages() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'outages'], () => outageApi.getOutages())
  return { outages: asList(data), loading, error, refetch }
}

export function useSafetyBriefing() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'safety-briefing'], visitorApi.getSafetyBriefing)
  return { briefing: data, loading, error, refetch }
}

/**
 * What a visitor screen shows for a Visitor Management stop, or for a source and/or a zone
 * (all optional). A stop name wins: the backend resolves it to the zone and tank it shows.
 */
export function useVisitorSummary(sourceId, zoneId, stop) {
  const { data, loading, error, refetch } = useModuleQuery(
    ['watersupply', 'visitor-summary', stop || 'no-stop', sourceId || 'all', zoneId || 'all'],
    () => visitorApi.getSummary(stop
      ? { stop }
      : { sourceId: sourceId || undefined, zoneId: zoneId || undefined }),
  )
  return { summary: data, loading, error, refetch }
}

/** Visitor Management stops mapped to water zones and tanks. */
export function useVisitorStops() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'visitor-stops'], visitorApi.getStops)
  return { stops: asList(data), loading, error, refetch }
}

export function useLevelHistory(sourceId) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'level-history', sourceId ?? 'none'],
    () => waterSourceApi.getLevelHistory(sourceId),
    { enabled: Boolean(sourceId) },
  )
  return { history: asList(data), loading, error }
}

export function useLotConsumption(lotId) {
  const { data, loading, error, refetch } = useModuleQuery(
    ['watersupply', 'lot-consumption', lotId ?? 'none'],
    () => productTransformationApi.getConsumptionByLot(lotId),
    { enabled: Boolean(lotId) },
  )
  return { data, loading, error, refetch }
}

export function useLotCompliance(lotId) {
  const { data, loading, error, refetch } = useModuleQuery(
    ['watersupply', 'lot-compliance', lotId ?? 'none'],
    () => productTransformationApi.getQualityCompliance(lotId),
    { enabled: Boolean(lotId) },
  )
  return { data, loading, error, refetch }
}

/** Command journal of a device, most recent first. */
export function useValveCommands(deviceId) {
  const { data, loading, error, refetch } = useModuleQuery(
    ['watersupply', 'valve-commands', deviceId ?? 'none'],
    () => iotCommandApi.getCommands(deviceId),
    { enabled: Boolean(deviceId) },
  )
  return { commands: asList(data), loading, error, refetch }
}
