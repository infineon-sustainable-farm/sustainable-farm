import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from './client'
import { fetchPersons, PERSONS_ENDPOINT } from './persons'

vi.mock('./client', () => ({ apiClient: { get: vi.fn(() => Promise.resolve([])) } }))

describe('fetchPersons', () => {
  beforeEach(() => vi.mocked(apiClient.get).mockClear())

  it('reads the people directory of the staff module', () => {
    fetchPersons()
    expect(PERSONS_ENDPOINT).toBe('/api/staff/persons')
    expect(apiClient.get).toHaveBeenCalledWith('/api/staff/persons', undefined)
  })

  it('passes a trimmed search', () => {
    fetchPersons('  awa ')
    expect(apiClient.get).toHaveBeenCalledWith('/api/staff/persons', { params: { search: 'awa' } })
  })
})
