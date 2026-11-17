import { CircleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import Button from './Button'
import { cx } from './cx'

/**
 * Message shown when data could not load, as machinery: centred red text and a secondary retry
 * button. The kit adds an icon (never colour only) and role="alert".
 *
 * @param {object} props
 * @param {import('react').ReactNode} [props.message] - common:status.error by default.
 * @param {() => void} [props.onRetry] - shows the retry button.
 * @param {string} [props.retryLabel] - common:actions.retry by default.
 * @param {boolean} [props.retrying] - spinner on the retry button.
 */
function ErrorState({ message, onRetry, retryLabel, retrying = false, className }) {
  const { t } = useTranslation('common')
  return (
    <div className={cx('flex flex-col items-center justify-center py-16 text-center', className)}>
      <p role="alert" className="flex max-w-prose items-center gap-2 text-error-fg">
        <CircleAlert size={20} aria-hidden="true" className="shrink-0" />
        <span>{message ?? t('status.error')}</span>
      </p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} loading={retrying} className="mt-4">
          {retryLabel ?? t('actions.retry')}
        </Button>
      )}
    </div>
  )
}

export default ErrorState
