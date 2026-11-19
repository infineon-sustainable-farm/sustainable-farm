import { Info } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cx } from './cx'
import { focusRing, transition } from './styles'

/**
 * Sends the user to the module that owns a shared resource, when the wanted item is not in a
 * list: "Ask the Machinery module to add it." and a link that opens the owner's create form.
 * The module name is the translated `<moduleId>:name` of its navigation.
 *
 * @param {object} props
 * @param {{ moduleId: string, createPath: string }} props.owner - see app/ownerLinks.js.
 * @param {'empty' | 'more'} [props.variant] - `empty`: nothing to choose ("Ask the X module to
 *   add it."); `more`: discreet line under a filled list ("Not in the list? Ask the X module.").
 * @param {string} [props.id] - id read by aria-describedby of the related control.
 */
function OwnerHint({ owner, variant = 'empty', id, className }) {
  const { t } = useTranslation(['common', owner.moduleId])
  const module = t(`${owner.moduleId}:name`)

  return (
    <p id={id} className={cx('flex flex-wrap items-center gap-x-2 text-xs text-text-muted', className)}>
      <Info size={14} aria-hidden="true" className="shrink-0" />
      <span>{variant === 'more' ? t('owner.notInList', { module }) : t('owner.askToAdd', { module })}</span>
      <Link
        to={owner.createPath}
        className={cx(
          'inline-flex items-center rounded-control font-medium text-text-accent underline underline-offset-2',
          'pointer-coarse:min-h-11',
          focusRing,
          transition,
        )}
      >
        {t('owner.addIn', { module })}
      </Link>
    </p>
  )
}

export default OwnerHint
