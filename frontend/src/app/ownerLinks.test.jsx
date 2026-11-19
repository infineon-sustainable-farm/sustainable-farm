// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { SUPPORTED_LANGUAGES } from '../shared/i18n/config'
import { CREATE_PARAM } from '../shared/hooks/useCreateParam'
import { loadRealModuleFiles } from '../test/appFixtures'
import { OWNER_LINKS } from './ownerLinks'

const navs = Object.values(await loadRealModuleFiles())
  .map((file) => file.nav)
  .filter(Boolean)
const locales = import.meta.glob('../locales/*/*.json', { eager: true, import: 'default' })

describe('ownerLinks', () => {
  it('names one owner per shared resource', () => {
    expect(Object.keys(OWNER_LINKS).sort()).toEqual(
      ['equipment', 'farm', 'field', 'harvest', 'packagedLot', 'person', 'processingBatch', 'variety', 'zone'].sort(),
    )
  })

  it.each(Object.entries(OWNER_LINKS))('%s links to a screen of its owner module, with a create value', (_, owner) => {
    const nav = navs.find((candidate) => candidate.id === owner.moduleId)
    expect(nav).toBeDefined()
    const url = new URL(owner.createPath, 'http://farm.test')
    expect(nav.items.map((item) => item.to)).toContain(url.pathname)
    expect(url.searchParams.get(CREATE_PARAM)).toBeTruthy()
  })

  it.each(Object.entries(OWNER_LINKS))('%s has an owner module name in every language', (_, owner) => {
    for (const language of SUPPORTED_LANGUAGES) {
      expect(locales[`../locales/${language}/${owner.moduleId}.json`]?.name).toBeTruthy()
    }
  })
})
