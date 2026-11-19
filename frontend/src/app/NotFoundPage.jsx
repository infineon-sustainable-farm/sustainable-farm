import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { PageHeader } from '../shared/ui'
import { primaryLinkButton } from './styles'

/** Page of an unknown URL, inside the shell: title, short text and a link back to the launcher. */
export default function NotFoundPage() {
  const { t } = useTranslation('common')
  return (
    <div className="p-6">
      <PageHeader title={t('notFound.title')} subtitle={t('notFound.description')} />
      <Link to="/" className={`mt-6 ${primaryLinkButton}`}>
        {t('notFound.backToModules')}
      </Link>
    </div>
  )
}
