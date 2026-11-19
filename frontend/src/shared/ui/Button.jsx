import { forwardRef } from 'react'
import { Loader2 } from 'lucide-react'
import { cx } from './cx'
import { focusRing, transition } from './styles'

// Machinery: primary "bg-primary text-white px-4 py-2 rounded-lg", secondary "border border-gray-300",
// danger "bg-red-600 text-white". Hover keeps the colour of a filled button; pressing darkens it
// and scales it to 0.98. The label stays on one line: the rows that hold buttons (page header,
// dialog footer) wrap them onto a new line instead. A full-width button takes the width of its
// container, so its label may wrap there rather than overflow.
const VARIANTS = {
  primary: 'bg-primary text-on-primary enabled:active:bg-primary-pressed',
  secondary:
    'border border-border-input bg-transparent text-text enabled:hover:bg-surface-muted enabled:active:bg-neutral-bg',
  danger: 'bg-danger text-on-primary enabled:active:bg-danger-pressed',
  ghost: 'bg-transparent text-text-accent enabled:hover:bg-surface-muted enabled:active:bg-neutral-bg',
}

// md is the machinery button (40 px); on touch screens every size grows to 44 px.
const SIZES = {
  sm: 'min-h-9 px-3 py-1 text-sm',
  md: 'min-h-10 px-4 py-2',
  lg: 'min-h-11 px-6 py-2',
}

const ICON_SIZES = { sm: 16, md: 16, lg: 18 }

/**
 * Action button of the kit.
 *
 * @param {object} props
 * @param {'primary'|'secondary'|'danger'|'ghost'} [props.variant='primary']
 * @param {'sm'|'md'|'lg'} [props.size='md']
 * @param {import('react').ComponentType} [props.icon] - lucide icon shown before the label.
 * @param {boolean} [props.loading] - shows a spinner, sets aria-busy and blocks clicks.
 * @param {boolean} [props.fullWidth]
 */
const Button = forwardRef(function Button(
  {
    variant = 'primary',
    size = 'md',
    icon: Icon,
    loading = false,
    disabled = false,
    fullWidth = false,
    type = 'button',
    className,
    children,
    ...rest
  },
  ref,
) {
  const iconSize = ICON_SIZES[size] ?? 16
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cx(
        'inline-flex cursor-pointer items-center justify-center gap-2 rounded-control pointer-coarse:min-h-11',
        'enabled:active:scale-98 motion-reduce:enabled:active:scale-100',
        'disabled:cursor-not-allowed disabled:opacity-60',
        VARIANTS[variant] ?? VARIANTS.primary,
        SIZES[size] ?? SIZES.md,
        fullWidth ? 'w-full' : 'whitespace-nowrap',
        focusRing,
        transition,
        className,
      )}
      {...rest}
    >
      {loading ? (
        <Loader2 size={iconSize} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
      ) : (
        Icon && <Icon size={iconSize} aria-hidden="true" />
      )}
      {children}
    </button>
  )
})

export default Button
