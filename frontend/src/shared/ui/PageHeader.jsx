import { cx } from './cx'

/**
 * Header of a screen, as the machinery dashboard: h1 "text-2xl font-bold", subtitle
 * "text-sm text-gray-500", actions (primary button) on the right, wrapping under on small screens.
 *
 * @param {object} props
 * @param {import('react').ReactNode} props.title
 * @param {import('react').ReactNode} [props.subtitle]
 * @param {import('react').ReactNode} [props.meta] - quiet text on the right (date of the dashboard).
 * @param {import('react').ReactNode} [props.actions]
 */
function PageHeader({ title, subtitle, meta, actions, className }) {
  return (
    <header className={cx('flex flex-wrap items-end justify-between gap-3', className)}>
      <div>
        <h1 className="text-2xl font-bold">{title}</h1>
        {subtitle && <p className="text-sm text-text-muted">{subtitle}</p>}
      </div>
      {(meta || actions) && (
        <div className="flex flex-wrap items-center gap-3">
          {meta && <p className="text-sm text-text-muted">{meta}</p>}
          {actions}
        </div>
      )}
    </header>
  )
}

export default PageHeader
