import { useTranslation } from 'react-i18next'
import { isRouteErrorResponse, Link, useRouteError } from 'react-router-dom'
import { ErrorState } from '../shared/ui'
import NotFoundPage from './NotFoundPage'
import { primaryLinkButton } from './styles'

/**
 * Router errorElement: a 404 response shows the 404 page, any other error the kit's ErrorState
 * and a link back to the launcher. `fullScreen` when it replaces the whole application (error
 * above the shell), otherwise it renders in the shell's main area.
 */
export default function RouteError({ fullScreen = false }) {
  const { t } = useTranslation('common')
  const error = useRouteError()
  const notFound = isRouteErrorResponse(error) && error.status === 404

  return (
    <div className={fullScreen ? 'min-h-dvh bg-surface' : undefined}>
      {notFound ? (
        <NotFoundPage />
      ) : (
        <div className="flex flex-col items-center p-6">
          <ErrorState />
          <Link to="/" className={primaryLinkButton}>
            {t('notFound.backToModules')}
          </Link>
        </div>
      )}
    </div>
  )
}
