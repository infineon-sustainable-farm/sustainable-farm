import { cx } from './cx'

// Machinery card: "bg-white rounded-2xl border border-gray-100 shadow-sm p-5"; title
// "text-lg font-semibold", subtitle "text-sm text-gray-500", then 16 px before the content.
const PADDINGS = { none: '', md: 'p-5' }

/**
 * Surface that groups content.
 *
 * @param {object} props
 * @param {import('react').ReactNode} [props.title] - rendered as a heading (h2 by default).
 * @param {import('react').ReactNode} [props.subtitle]
 * @param {import('react').ReactNode} [props.actions] - shown on the right of the title.
 * @param {'h2'|'h3'} [props.titleAs='h2']
 * @param {'none'|'md'} [props.padding='md'] - none for a table that fills the card.
 * @param {string} [props.as='section'] - element of the card.
 */
function Card({
  title,
  subtitle,
  actions,
  titleAs: Heading = 'h2',
  padding = 'md',
  as: Element = 'section',
  className,
  children,
  ...rest
}) {
  const hasHeader = title || subtitle || actions
  return (
    <Element
      className={cx(
        'rounded-card border border-border-subtle bg-surface-raised shadow-raised',
        PADDINGS[padding] ?? PADDINGS.md,
        className,
      )}
      {...rest}
    >
      {hasHeader && (
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            {title && <Heading className="text-lg font-semibold">{title}</Heading>}
            {subtitle && <p className="text-sm text-text-muted">{subtitle}</p>}
          </div>
          {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </Element>
  )
}

export default Card
