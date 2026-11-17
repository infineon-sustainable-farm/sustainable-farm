import { describe, expect, it } from 'vitest'
import i18n from './i18n'
import { namespaces, resources } from './resources'

const MODULES = [
  'machinery',
  'plants',
  'producttransformation',
  'energysupply',
  'watersupply',
  'salesmarketing',
  'visitormanagement',
]

describe('i18n setup', () => {
  it('loads common and one namespace per module in the three languages', () => {
    expect(namespaces).toEqual(['common', ...MODULES].sort())
    expect(Object.keys(resources).sort()).toEqual(['de', 'en', 'fr'])
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
