/**
 * Chart colours, from the chart tokens of src/index.css: primary series, warning (orange),
 * success (green), alert (red) and information (blue), grid and axis text.
 */
export const C = {
  primary: 'var(--color-chart-1)',
  orange: 'var(--color-chart-2)',
  green: 'var(--color-chart-4)',
  red: 'var(--color-chart-5)',
  blue: 'var(--color-chart-6)',
  line: 'var(--color-chart-grid)',
  muted: 'var(--color-chart-3)',
}

/* ==================== Chart exports ==================== */

/** Triggers the browser-side download of a Blob. */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const QUOTE = '"'
const CSV_SPECIALS = [QUOTE, ';', '\r', '\n']

/**
 * Converts data rows to CSV (semicolon separator, UTF-8 BOM).
 * The semicolon and the BOM are required so that Excel displays
 * accents correctly and splits columns without an import wizard.
 *
 * @param {Array<Object>} rows
 * @param {Array<{ key: string, label: string }>} columns
 */
export function toCsv(rows, columns) {
  const escape = (value) => {
    const text = value === null || value === undefined ? '' : String(value)
    return CSV_SPECIALS.some((special) => text.includes(special)) ? QUOTE + text.replaceAll(QUOTE, QUOTE + QUOTE) + QUOTE : text
  }
  const header = columns.map((column) => escape(column.label)).join(';')
  const body = (rows || []).map((row) => columns.map((column) => escape(row[column.key])).join(';'))
  return `\uFEFF${[header, ...body].join('\r\n')}\r\n`
}

/** Downloads a dataset as CSV (per-chart export). */
export function exportRowsAsCsv(rows, columns, filename) {
  downloadBlob(new Blob([toCsv(rows, columns)], { type: 'text/csv;charset=utf-8' }), filename)
}

/**
 * Captures the SVG rendered by Recharts and downloads it as PNG.
 *
 * @param {HTMLElement} container - element holding the chart SVG.
 * @param {string} filename
 * @returns {boolean} true if an SVG was exported.
 */
export function exportChartAsPng(container, filename) {
  const svg = container?.querySelector('svg')
  if (!svg) return false

  const width = Math.max(320, Math.round(svg.getBoundingClientRect().width || 640))
  const height = Math.max(200, Math.round(svg.getBoundingClientRect().height || 320))
  const clone = svg.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  clone.setAttribute('width', String(width))
  clone.setAttribute('height', String(height))

  const source = `<?xml version="1.0" standalone="no"?>${new XMLSerializer().serializeToString(clone)}`
  const image = new Image()
  const svgUrl = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(source)}`

  image.onload = () => {
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) return
    // White background of the surface token: a transparent PNG reads poorly in a report.
    context.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--color-surface-raised').trim() || 'white'
    context.fillRect(0, 0, width, height)
    context.drawImage(image, 0, 0, width, height)
    canvas.toBlob((blob) => {
      if (blob) downloadBlob(blob, filename)
    }, 'image/png')
  }
  image.src = svgUrl
  return true
}
