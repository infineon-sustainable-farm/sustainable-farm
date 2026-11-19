import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { cx } from './cx'
import { focusRing } from './styles'

// Machinery DashboardKpiCard: 40 px icon tile (rounded-xl) tinted per tone, 30 px bold value, label,
// hint. The tile icons use the -fg state tokens (machinery used amber-600 and red-600).
const TONES = {
  primary: 'bg-primary-tint text-primary',
  warning: 'bg-warning-tint text-warning-fg',
  error: 'bg-error-tint text-error-fg',
  info: 'bg-info-tint text-info-fg',
}

/**
 * Key figure tile. With `to` the whole card is a link (as on the machinery dashboard).
 *
 * @param {object} props
 * @param {import('react').ComponentType} props.icon - lucide icon of the tile.
 * @param {import('react').ReactNode} props.label
 * @param {import('react').ReactNode} props.value
 * @param {import('react').ReactNode} [props.hint]
 * @param {'primary'|'warning'|'error'|'info'} [props.tone='primary']
 * @param {string} [props.to] - route the card opens.
 */
function KpiCard({ icon: Icon, label, value, hint, tone = 'primary', to, className }) {
  const frame = cx(
    'group flex flex-col gap-4 rounded-card border border-border-subtle bg-surface-raised p-5 shadow-raised',
    to &&
      'transition duration-(--duration-fast) ease-standard hover:-translate-y-0.5 hover:shadow-overlay motion-reduce:transition-none motion-reduce:hover:translate-y-0',
    to && focusRing,
    className,
  )

  const content = (
    <>
      <div className="flex items-center justify-between">
        <span className={cx('flex h-10 w-10 items-center justify-center rounded-tile', TONES[tone] ?? TONES.primary)}>
          <Icon size={20} aria-hidden="true" />
        </span>
        {to && (
          // Decorative arrow, gray-300 as in machinery (border-input is that gray).
          <ArrowRight
            size={16}
            aria-hidden="true"
            className="text-border-input transition duration-(--duration-fast) group-hover:translate-x-0.5 group-hover:text-primary motion-reduce:transition-none"
          />
        )}
      </div>
      <div>
        <p className="text-3xl font-bold tracking-tight">{value}</p>
        <p className="mt-0.5 text-sm font-medium text-text">{label}</p>
        {hint && <p className="mt-1 text-xs text-text-muted">{hint}</p>}
      </div>
    </>
  )

  if (to) {
    return (
      <Link to={to} className={frame}>
        {content}
      </Link>
    )
  }
  return <div className={frame}>{content}</div>
}

export default KpiCard
