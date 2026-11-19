import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../../shared/api/client.js', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

import { apiClient } from '../../../shared/api/client.js'
import * as api from './watersupplyApi.js'

const FARM_CLIENTS = ['farmApi', 'fieldApi', 'zoneApi', 'zoneBlockMappingApi']
// Resources picked from their owner module (app/ownerLinks.js), read only.
const OWNER_CLIENTS = {
  processingBatchApi: /^\/api\/product-transformation\/batches$/,
  varietyCatalogApi: /^\/api\/plants\/variety-references$/,
}

function calledPaths() {
  return Object.values(apiClient).flatMap((method) => method.mock.calls.map((call) => call[0]))
}

beforeEach(() => {
  vi.clearAllMocks()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('watersupply api paths', () => {
  it('serves every water supply endpoint under /api/water-supply', () => {
    for (const [name, client] of Object.entries(api)) {
      if (typeof client !== 'object' || FARM_CLIENTS.includes(name) || name in OWNER_CLIENTS) continue
      for (const call of Object.values(client)) call(7, 7)
    }
    const paths = calledPaths()
    expect(paths.length).toBeGreaterThan(60)
    for (const path of paths) expect(path).toMatch(/^\/api\/water-supply\//)
  })

  it('reads farms, fields, zones and zone block mappings from the farm module', () => {
    for (const name of FARM_CLIENTS) {
      for (const call of Object.values(api[name])) call(7, {})
    }
    for (const path of calledPaths()) expect(path).toMatch(/^\/api\/farm\//)
  })

  it('reads the lots and the varieties from their owner modules, without writing', () => {
    for (const [name, path] of Object.entries(OWNER_CLIENTS)) {
      vi.clearAllMocks()
      for (const call of Object.values(api[name])) call()
      expect(apiClient.get).toHaveBeenCalled()
      for (const called of calledPaths()) expect(called).toMatch(path)
      expect(apiClient.post).not.toHaveBeenCalled()
    }
  })

  it('keeps the resource segments and numeric ids', () => {
    api.irrigationApi.startIrrigation(42)
    api.irrigationApi.getLogs()
    api.waterQuotaApi.getUsage('2026-06-01')
    api.farmApi.getFarmFields(3)
    api.iotCommandApi.sendCommand('valve 1', { command: 'CLOSE_VALVE' })
    api.healthApi.check()

    expect(apiClient.post).toHaveBeenCalledWith('/api/water-supply/irrigations/42/start')
    expect(apiClient.get).toHaveBeenCalledWith('/api/water-supply/irrigation-logs')
    expect(apiClient.get).toHaveBeenCalledWith('/api/water-supply/quotas/usage?month=2026-06-01')
    expect(apiClient.get).toHaveBeenCalledWith('/api/farm/farms/3/fields')
    expect(apiClient.post).toHaveBeenCalledWith('/api/water-supply/iot/devices/valve%201/commands', {
      command: 'CLOSE_VALVE',
    })
    expect(apiClient.get).toHaveBeenCalledWith('/api/water-supply/health')
  })

  it('downloads the CSV exports from the water supply reports', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 503 })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.downloadCsv('consumption', 'week')).rejects.toThrow('HTTP 503')
    expect(fetchMock).toHaveBeenCalledWith('/api/water-supply/reports/consumption/csv?period=week')
  })

  it('never fetches an export that is not one of the reports', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.downloadCsv('http://127.0.0.1/admin')).rejects.toThrow('Unknown export')
    await expect(api.downloadCsv('//169.254.169.254/latest/meta-data')).rejects.toThrow('Unknown export')
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
