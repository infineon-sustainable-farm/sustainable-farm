import { forwardRef } from 'react'
import { cx } from './cx'
import { focusRing, transition } from './styles'

// Machinery table actions: "text-primary" (edit) and "text-red-600" (delete) icons of 16 px. The kit
// gives them a 32 px target (44 px on touch screens), a hover surface and the focus mark.
const TONES = {
  primary: 'text-text-accent',
  danger: 'text-error-fg',
  neutral: 'text-text-muted enabled:hover:text-text',
}

const SIZES = {
  sm: { box: 'h-8 w-8 pointer-coarse:h-11 pointer-coarse:w-11', icon: 16 },
  md: { box: 'h-10 w-10 pointer-coarse:h-11 pointer-coarse:w-11', icon: 18 },
}

/**
 * Button made of an icon only. `label` is required: it is the accessible name (aria-label) and
 * the tooltip, so pass a translated text, t('...').
 *
 * @param {object} props
 * @param {import('react').ComponentType} props.icon - lucide icon.
 * @param {string} props.label - translated accessible name.
 * @param {'primary'|'danger'|'neutral'} [props.tone='primary']
 * @param {'sm'|'md'} [props.size='sm']
 */
const IconButton = forwardRef(function IconButton(
  { icon: Icon, label, tone = 'primary', size = 'sm', type = 'button', className, ...rest },
  ref,
) {
  if (!label) throw new Error('IconButton: the label prop (translated accessible name) is required')
  const { box, icon } = SIZES[size] ?? SIZES.sm
  return (
    <button
      ref={ref}
      type={type}
      aria-label={label}
      title={label}
      className={cx(
        'inline-flex shrink-0 cursor-pointer items-center justify-center rounded-control',
        'enabled:hover:bg-surface-muted enabled:active:scale-98 enabled:active:bg-neutral-bg',
        'motion-reduce:enabled:active:scale-100 disabled:cursor-not-allowed disabled:opacity-60',
        box,
        TONES[tone] ?? TONES.primary,
        focusRing,
        transition,
        className,
      )}
      {...rest}
    >
      <Icon size={icon} aria-hidden="true" />
    </button>
  )
})

export default IconButton
