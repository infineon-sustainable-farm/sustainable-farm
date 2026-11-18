import { describe, expect, it } from 'vitest'
import { sameId } from './ids'

describe('sameId', () => {
  it('matches a numeric API id with the string held by a form control', () => {
    expect(sameId(5, '5')).toBe(true)
    expect(sameId('5', 5)).toBe(true)
    expect(sameId(5, 5)).toBe(true)
  })

  it('never matches an empty or missing id', () => {
    expect(sameId(5, '')).toBe(false)
    expect(sameId(null, null)).toBe(false)
    expect(sameId(undefined, '5')).toBe(false)
    expect(sameId(5, '6')).toBe(false)
  })
})
