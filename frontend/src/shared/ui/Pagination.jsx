import { useTranslation } from 'react-i18next'
import { cx } from './cx'
import { focusRing, transition } from './styles'

// Machinery pagination (former shared/components/Pagination): right aligned, "Page x of y" in gray, then
// Previous and Next "px-3 py-1 rounded-lg border border-gray-200 hover:bg-gray-50", 40 % opacity
// when disabled. The kit adds a named navigation landmark, the focus mark and 44 px targets on
// touch screens.
const buttonClass = cx(
  'inline-flex min-h-7 cursor-pointer items-center rounded-control border border-border-divider px-3 py-1 pointer-coarse:min-h-11',
  'enabled:hover:bg-surface-muted enabled:active:scale-98 enabled:active:bg-neutral-bg motion-reduce:enabled:active:scale-100',
  'disabled:cursor-not-allowed disabled:opacity-40',
  focusRing,
  transition,
)

/**
 * Previous and next page buttons. Pages are 0-based, as the API pages.
 *
 * @param {object} props
 * @param {number} props.page - current page, from 0.
 * @param {number} props.totalPages
 * @param {(page: number) => void} props.onPageChange
 * @param {string} [props.label] - name of the navigation, common:pagination.label by default.
 */
function Pagination({ page, totalPages, onPageChange, label, className }) {
  const { t } = useTranslation('common')
  if (!totalPages || totalPages < 1) return null
  const isFirstPage = page <= 0
  const isLastPage = page >= totalPages - 1

  return (
    <nav aria-label={label ?? t('pagination.label')} className={cx('mt-4 flex items-center justify-end gap-3 text-sm', className)}>
      <span className="text-text-muted">
        {t('pagination.pageOf', { page: page + 1, total: totalPages })}
      </span>
      <button
        type="button"
        disabled={isFirstPage}
        onClick={() => onPageChange(page - 1)}
        className={buttonClass}
      >
        {t('pagination.previous')}
      </button>
      <button
        type="button"
        disabled={isLastPage}
        onClick={() => onPageChange(page + 1)}
        className={buttonClass}
      >
        {t('pagination.next')}
      </button>
    </nav>
  )
}

export default Pagination
