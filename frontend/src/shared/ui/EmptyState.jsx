import { useTranslation } from 'react-i18next'
import Button from './Button'
import { cx } from './cx'

/**
 * Message shown instead of an empty list, as machinery: centred gray text, 64 px above and below,
 * an optional primary action.
 *
 * @param {object} props
 * @param {import('react').ReactNode} [props.message] - common:status.noResults by default.
 * @param {import('react').ReactNode} [props.title]
 * @param {import('react').ComponentType} [props.icon] - lucide icon above the text.
 * @param {string} [props.actionLabel]
 * @param {() => void} [props.onAction]
 * @param {import('react').ReactNode} [props.action] - custom action, replaces actionLabel and onAction.
 */
function EmptyState({ message, title, icon: Icon, actionLabel, onAction, action, className }) {
  const { t } = useTranslation('common')
  return (
    <div className={cx('flex flex-col items-center justify-center py-16 text-center text-text-muted', className)}>
      {Icon && <Icon size={28} aria-hidden="true" className="mb-2" />}
      {title && <p className="mb-1 font-semibold text-text">{title}</p>}
      <p className="max-w-prose">{message ?? t('status.noResults')}</p>
      {(action || actionLabel) && (
        <div className="mt-4">{action ?? <Button onClick={onAction}>{actionLabel}</Button>}</div>
      )}
    </div>
  )
}

export default EmptyState
