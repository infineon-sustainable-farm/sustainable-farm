export const C = {
  primary: 'var(--ws-primary, #0a8276)',
  orange: 'var(--ws-orange, #ef6c00)',
  green: 'var(--ws-green, #4caf50)',
  red: 'var(--ws-red, #c62828)',
  blue: 'var(--ws-blue, #1565c0)',
  line: 'var(--ws-line, #e2e9e7)',
  muted: 'var(--ws-muted, #6b7a78)',
}

/**
 * Formats a volume in litres with the matching unit (L, m3 above 10,000 L).
 *
 * @param {number} liters
 * @param {number} [decimals]
 */
export function formatLiters(liters, decimals = 0) {
  const value = Number(liters) || 0
  if (Math.abs(value) >= 10000) return `${(value / 1000).toFixed(1)} m3`
  return `${value.toFixed(decimals)} L`
}

/** Formats an amount in millimetres (rainfall, ET0). */
export function formatMm(mm, decimals = 1) {
  return `${(Number(mm) || 0).toFixed(decimals)} mm`
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
    return /[";\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
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
 * The background is forced to white: a transparent PNG reads poorly in a report.
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
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, width, height)
    context.drawImage(image, 0, 0, width, height)
    canvas.toBlob((blob) => {
      if (blob) downloadBlob(blob, filename)
    }, 'image/png')
  }
  image.src = svgUrl
  return true
}
