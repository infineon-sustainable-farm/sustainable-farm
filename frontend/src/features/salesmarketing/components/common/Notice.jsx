import { CircleAlert, CircleCheck, Info, RefreshCw, TriangleAlert, WifiOff, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, IconButton } from '../../../../shared/ui';

// Banners of the sales and marketing screens: load failure with Retry, failed or successful
// action with a close button, information. Same tones as the kit badges.
const TONES = {
  error: { className: 'bg-error-tint text-error-fg', icon: CircleAlert, role: 'alert' },
  warning: { className: 'bg-warning-tint text-warning-fg', icon: TriangleAlert, role: 'alert' },
  offline: { className: 'bg-warning-tint text-warning-fg', icon: WifiOff, role: 'alert' },
  success: { className: 'bg-success-bg text-success-fg', icon: CircleCheck, role: 'status' },
  info: { className: 'bg-info-tint text-info-fg', icon: Info, role: 'status' },
};

/**
 * @param {object} props
 * @param {'error'|'warning'|'offline'|'success'|'info'} [props.tone='error']
 * @param {import('react').ReactNode} [props.title] - bold line above the message.
 * @param {import('react').ReactNode} props.children - message.
 * @param {() => void} [props.onRetry] - shows a Retry button.
 * @param {string} [props.retryLabel] - label of the Retry button, common:actions.retry by default.
 * @param {() => void} [props.onDismiss] - shows a close button.
 */
function Notice({ tone = 'error', title, children, onRetry, retryLabel, onDismiss, className }) {
  const { t } = useTranslation('common');
  const { className: toneClass, icon: Icon, role } = TONES[tone] ?? TONES.error;
  return (
    <div role={role} className={['flex items-start gap-3 rounded-control px-4 py-3 text-sm', toneClass, className].filter(Boolean).join(' ')}>
      <Icon size={18} aria-hidden="true" className="mt-0.5 shrink-0" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-semibold">{title}</p>}
        <div className="break-words">{children}</div>
      </div>
      {onRetry && (
        <Button size="sm" variant="secondary" icon={RefreshCw} onClick={onRetry}>
          {retryLabel ?? t('actions.retry')}
        </Button>
      )}
      {onDismiss && <IconButton icon={X} tone="neutral" label={t('actions.dismiss')} onClick={onDismiss} />}
    </div>
  );
}

export default Notice;
