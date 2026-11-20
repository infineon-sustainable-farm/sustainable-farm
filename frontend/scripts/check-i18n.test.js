// @vitest-environment node
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { checkLocales } from './check-i18n.mjs'

const fixture = (name) => fileURLToPath(new URL(`./__fixtures__/i18n/${name}`, import.meta.url))
const script = fileURLToPath(new URL('./check-i18n.mjs', import.meta.url))
const run = (...args) => spawnSync(process.execPath, [script, ...args], { encoding: 'utf8' })

describe('check-i18n', () => {
  it('accepts languages that share the same keys', () => {
    expect(checkLocales(fixture('valid'))).toEqual([])
  })

  it('lists every parity problem of a broken fixture', () => {
    const problems = checkLocales(fixture('broken'))
    expect(problems).toEqual(
      expect.arrayContaining([
        'en/common.json: "nested.count" must be a string, found number',
        'de/common.json: missing key "nested.save" (present in en)',
        'de/machinery.json: namespace is missing',
        'fr/common.json: "title" is empty',
        'fr/common.json: extra key "nested.extra" (absent from en)',
      ]),
    )
    expect(problems).toHaveLength(5)
  })

  it('reports a missing locales directory', () => {
    expect(checkLocales(fixture('does-not-exist'))).toHaveLength(1)
  })

  it('exits 0 on the real translations and 1 on the broken fixture', () => {
    const ok = run()
    expect(ok.status, ok.stderr).toBe(0)

    const broken = run(fixture('broken'))
    expect(broken.status).toBe(1)
    expect(broken.stderr).toContain('i18n check failed, 5 problem(s)')
    expect(broken.stderr).toContain('de/machinery.json: namespace is missing')
  })
})
