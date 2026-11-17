// Translations live in src/locales/<language>/<namespace>.json: one file per
// module and per language, so parallel work on different modules never edits
// the same file. Every JSON file found here becomes a namespace.
const files = import.meta.glob('../../locales/*/*.json', { eager: true, import: 'default' })

export function buildResources(entries) {
  const resources = {}
  for (const [path, messages] of Object.entries(entries)) {
    const match = path.match(/locales\/([^/]+)\/([^/]+)\.json$/)
    if (!match) continue
    const [, language, namespace] = match
    resources[language] ??= {}
    resources[language][namespace] = messages
  }
  return resources
}

export const resources = buildResources(files)
export const namespaces = [...new Set(Object.values(resources).flatMap((byNs) => Object.keys(byNs)))].sort()
