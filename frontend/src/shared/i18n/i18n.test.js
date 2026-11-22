// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import i18n from './i18n'
import { availableLanguages, namespaces, resources } from './resources'
import { collectModules } from '../../app/modules'
import { loadRealModuleFiles } from '../../test/appFixtures'

// Generic on purpose: every integrated module (one that exports a `nav` from its routes.jsx) has
// its own namespace, so a new module ships its translations without touching this test.
const MODULES = collectModules(await loadRealModuleFiles())
  .map((module) => module.nav?.id)
  .filter(Boolean)

describe('i18n setup', () => {
  it('has common and one namespace per module in the three languages, English bundled', () => {
    expect(namespaces).toEqual(['common', ...MODULES].sort())
    expect(availableLanguages).toEqual(['de', 'en', 'fr'])
    expect(Object.keys(resources)).toEqual(['en'])
  })

  it('translates common keys and falls back to English', async () => {
    expect(i18n.t('notFound.title')).toBe('Page not found')
    await i18n.changeLanguage('de')
    expect(i18n.t('notFound.backToModules')).toBe('Zurück zu den Modulen')
    await i18n.changeLanguage('fr-CA')
    expect(i18n.resolvedLanguage).toBe('fr')
    expect(i18n.t('name', { ns: 'salesmarketing' })).toBe('Ventes et marketing')
    await i18n.changeLanguage('es')
    expect(i18n.resolvedLanguage).toBe('en')
    expect(i18n.t('appName')).toBe('Sustainable Farm')
  })
})
