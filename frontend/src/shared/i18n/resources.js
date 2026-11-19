// Translations live in src/locales/<language>/<namespace>.json: one file per
// module and per language, so parallel work on different modules never edits
// the same file. Every JSON file found here becomes a namespace.
//
// English, the fallback language (config.js), is bundled: the first render never waits for it.
// The other languages load on demand, one chunk per file, through `localeBackend` below.
// import.meta.glob takes literal patterns only, hence "en" written out here.
const bundledFiles = import.meta.glob('../../locales/en/*.json', { eager: true, import: 'default' })
const lazyFiles = import.meta.glob(['../../locales/*/*.json', '!../../locales/en/*.json'], {
  import: 'default',
})

function parsePath(path) {
  const match = path.match(/locales\/([^/]+)\/([^/]+)\.json$/)
  return match ? { language: match[1], namespace: match[2] } : null
}

export function buildResources(entries) {
  const resources = {}
  for (const [path, messages] of Object.entries(entries)) {
    const parsed = parsePath(path)
    if (!parsed) continue
    resources[parsed.language] ??= {}
    resources[parsed.language][parsed.namespace] = messages
  }
  return resources
}

/** Bundled resources: the fallback language only. */
export const resources = buildResources(bundledFiles)
export const namespaces = Object.keys(Object.values(resources)[0] ?? {}).sort()

/** Languages with translation files, the bundled one and the ones loaded on demand. */
export const availableLanguages = [
  ...new Set([...Object.keys(bundledFiles), ...Object.keys(lazyFiles)].map((path) => parsePath(path)?.language)),
]
  .filter(Boolean)
  .sort()

/**
 * i18next backend loading a language and namespace that is not bundled. A missing file resolves
 * to an empty bundle: i18next then falls back to English for its keys.
 */
export const localeBackend = {
  type: 'backend',
  init() {},
  read(language, namespace, callback) {
    const load = lazyFiles[`../../locales/${language}/${namespace}.json`]
    if (!load) {
      callback(null, {})
      return
    }
    load().then(
      (messages) => callback(null, messages),
      (error) => callback(error, null),
    )
  },
}
