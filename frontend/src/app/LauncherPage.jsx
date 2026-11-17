import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import LanguageSwitcher from './LanguageSwitcher'
import { focusOnSurface, shellTransition } from './styles'

/**
 * Launcher at `/` (A4, Q7): full screen, no sidebar. Logo, application name, language switcher and
 * one tile per integrated module: a primary rounded square with the module icon in white and the
 * translated name below, nothing else. The tiles wrap in a centred row of at most four (the last
 * row is centred, C-LAY-03), two per row at 320 px.
 *
 * @param {{ navs: object[] }} props - `nav` exports of the integrated modules.
 */
export default function LauncherPage({ navs }) {
  const { t } = useTranslation('common')

  return (
    <div className="flex min-h-dvh flex-col bg-surface px-4 py-6">
      <div className="flex justify-end">
        <LanguageSwitcher className="w-40" />
      </div>

      <main className="flex flex-1 flex-col items-center justify-center gap-10 py-10">
        <header className="flex flex-col items-center gap-3 text-center">
          <img src="/logo.webp" alt="" className="h-auto w-20" />
          <h1 className="text-2xl font-bold">{t('appName')}</h1>
        </header>

        <nav aria-label={t('launcher.modules')} className="w-full max-w-xl">
          <ul className="flex flex-wrap justify-center gap-6">
            {navs.map((nav) => {
              const Icon = nav.icon
              return (
                <li key={nav.id}>
                  <Link
                    to={nav.basePath}
                    className={`group flex w-28 flex-col items-center gap-2 rounded-tile p-1 text-center ${focusOnSurface}`}
                  >
                    <span
                      className={`flex size-20 items-center justify-center rounded-card bg-primary text-on-primary shadow-raised
                        group-hover:shadow-overlay group-active:scale-98 group-active:bg-primary-pressed
                        motion-reduce:group-active:scale-100 ${shellTransition}`}
                    >
                      <Icon size={36} aria-hidden="true" />
                    </span>
                    <span className="text-sm font-medium text-text">{t(nav.nameKey)}</span>
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>
      </main>
    </div>
  )
}
