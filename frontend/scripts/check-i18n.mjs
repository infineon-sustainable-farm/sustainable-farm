#!/usr/bin/env node
// Translation parity check, no dependencies.
//
// Layout: <localesDir>/<language>/<namespace>.json. Every namespace must exist
// in every supported language, with exactly the same set of keys, and every
// value must be a non-empty string. Exits 1 and lists the problems otherwise.
//
// Usage: node scripts/check-i18n.mjs [localesDir]   (default: src/locales)

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export const LANGUAGES = ['en', 'de', 'fr']
const REFERENCE = LANGUAGES[0]

function flatten(value, prefix, out, problems, where) {
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    const entries = Object.entries(value)
    if (entries.length === 0) problems.push(`${where}: "${prefix || '(root)'}" is an empty object`)
    for (const [key, child] of entries) flatten(child, prefix ? `${prefix}.${key}` : key, out, problems, where)
    return out
  }
  out.add(prefix)
  if (typeof value !== 'string') {
    problems.push(`${where}: "${prefix}" must be a string, found ${Array.isArray(value) ? 'array' : typeof value}`)
  } else if (value.trim() === '') {
    problems.push(`${where}: "${prefix}" is empty`)
  }
  return out
}

function listNamespaces(dir) {
  return readdirSync(dir)
    .filter((name) => name.endsWith('.json'))
    .map((name) => name.slice(0, -'.json'.length))
    .sort()
}

/**
 * @param {string} localesDir
 * @returns {string[]} problems, empty when the translations are consistent
 */
export function checkLocales(localesDir) {
  const problems = []
  if (!existsSync(localesDir) || !statSync(localesDir).isDirectory()) {
    return [`${localesDir}: locales directory not found`]
  }

  const present = readdirSync(localesDir).filter((name) => statSync(join(localesDir, name)).isDirectory())
  for (const extra of present.filter((name) => !LANGUAGES.includes(name))) {
    problems.push(`${extra}/: unsupported language folder (expected ${LANGUAGES.join(', ')})`)
  }

  const keysByLanguage = {}
  for (const language of LANGUAGES) {
    const dir = join(localesDir, language)
    keysByLanguage[language] = {}
    if (!present.includes(language)) {
      problems.push(`${language}/: language folder is missing`)
      continue
    }
    for (const namespace of listNamespaces(dir)) {
      const where = `${language}/${namespace}.json`
      let data
      try {
        data = JSON.parse(readFileSync(join(dir, `${namespace}.json`), 'utf8'))
      } catch (error) {
        problems.push(`${where}: invalid JSON (${error.message})`)
        continue
      }
      keysByLanguage[language][namespace] = flatten(data, '', new Set(), problems, where)
    }
  }

  const allNamespaces = [...new Set(LANGUAGES.flatMap((l) => Object.keys(keysByLanguage[l])))].sort()
  for (const namespace of allNamespaces) {
    for (const language of LANGUAGES) {
      if (present.includes(language) && !existsSync(join(localesDir, language, `${namespace}.json`))) {
        problems.push(`${language}/${namespace}.json: namespace is missing`)
      }
    }
    const reference = keysByLanguage[REFERENCE][namespace]
    if (!reference) continue
    for (const language of LANGUAGES.slice(1)) {
      const keys = keysByLanguage[language][namespace]
      if (!keys) continue
      for (const key of reference) {
        if (!keys.has(key)) problems.push(`${language}/${namespace}.json: missing key "${key}" (present in ${REFERENCE})`)
      }
      for (const key of keys) {
        if (!reference.has(key)) problems.push(`${language}/${namespace}.json: extra key "${key}" (absent from ${REFERENCE})`)
      }
    }
  }
  return problems
}

const isMain = process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) {
  const defaultDir = fileURLToPath(new URL('../src/locales', import.meta.url))
  const localesDir = resolve(process.argv[2] ?? defaultDir)
  const problems = checkLocales(localesDir)
  if (problems.length > 0) {
    console.error(`i18n check failed, ${problems.length} problem(s) in ${localesDir}:`)
    for (const problem of problems) console.error(`  - ${problem}`)
    process.exit(1)
  }
  console.log(`i18n check passed: ${LANGUAGES.join(', ')} have the same keys in ${localesDir}`)
}
