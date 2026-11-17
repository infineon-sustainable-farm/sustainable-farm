import { cx } from './cx'

// Machinery badges: "rounded-full text-xs font-semibold", green, amber and red -100 with -700 text,
// gray-100 with gray-600 text for a neutral one (DueBadge).
const BADGE_TONES = {
  success: 'bg-success-bg text-success-fg',
  warning: 'bg-warning-bg text-warning-fg',
  error: 'bg-error-bg text-error-fg',
  info: 'bg-info-bg text-info-fg',
  neutral: 'bg-neutral-bg text-neutral-fg',
}

// sm: "px-2 py-0.5" (DueBadge, low stock); md: "px-2 py-1" (machinery StatusBadge).
const SIZES = { sm: 'py-0.5', md: 'py-1' }

/**
 * Small label for a state or a count. The text says the state: the colour only repeats it. With an
 * icon and no visible text, pass `label` so the badge still has a name.
 *
 * @param {object} props
 * @param {'success'|'warning'|'error'|'info'|'neutral'} [props.tone='neutral']
 * @param {import('react').ComponentType} [props.icon] - lucide icon before the text.
 * @param {string} [props.label] - accessible name when the badge shows an icon only.
 * @param {'sm'|'md'} [props.size='sm']
 */
function Badge({ tone = 'neutral', icon: Icon, label, size = 'sm', className, children, ...rest }) {
  const iconOnly = !children && Icon
  return (
    <span
      className={cx(
        'inline-flex items-center gap-1 whitespace-nowrap rounded-pill px-2 text-xs font-semibold',
        SIZES[size] ?? SIZES.sm,
        BADGE_TONES[tone] ?? BADGE_TONES.neutral,
        className,
      )}
      role={iconOnly ? 'img' : undefined}
      aria-label={iconOnly ? label : undefined}
      title={iconOnly ? label : undefined}
      {...rest}
    >
      {Icon && <Icon size={12} aria-hidden="true" />}
      {children}
    </span>
  )
}

export default Badge
