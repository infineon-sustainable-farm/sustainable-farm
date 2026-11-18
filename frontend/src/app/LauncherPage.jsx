import { ArrowRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { HOME_PHOTO_SIZES, HOME_PHOTO_SRC, HOME_PHOTO_SRCSET } from './homePhoto'
import LanguageSwitcher from './LanguageSwitcher'
import { shellTransition } from './styles'

// The cards sit over the photo or over the page background: the focus outline is drawn inside the
// white card, where the primary colour always passes 3:1.
const focusInsideCard =
  'focus-visible:outline-2 focus-visible:-outline-offset-4 focus-visible:outline-focus-ring'

/**
 * Home page at `/` (A4, Q7): one full-screen section with no sidebar. A landscape photo under a
 * plain dark scrim, a top bar (logo, application name, language switcher), a headline, and one
 * card per integrated module laid over the bottom of the photo. The cards come from the `nav`
 * exports of the modules; they sit in one row from 1440 px, four then three (centred) on tablets
 * and laptops, two per row on phones (the odd one centred), one per row under 384 px.
 *
 * Below 1024 px the photo covers the top bar and the headline block, which takes the height the
 * cards leave, and the cards overlap its bottom edge; from 1024 px it covers the whole section.
 *
 * @param {{ navs: object[] }} props - `nav` exports of the integrated modules.
 */
export default function LauncherPage({ navs }) {
  const { t } = useTranslation('common')

  return (
    <div className="relative flex min-h-dvh flex-col bg-surface">
      <div className="relative flex flex-1 flex-col pb-8 text-on-scrim lg:static">
        {/* Background of the headline block (of the whole page from 1024 px), under the content: the
            photo under the scrim (::after). It reaches 2rem under the block so the cards overlap its
            bottom edge. */}
        <div
          data-grille-decor
          className="absolute inset-x-0 top-0 -bottom-8 bg-text after:absolute after:inset-0 after:bg-scrim lg:bottom-0"
        >
          <img
            src={HOME_PHOTO_SRC}
            srcSet={HOME_PHOTO_SRCSET}
            sizes={HOME_PHOTO_SIZES}
            width="1440"
            height="900"
            alt=""
            fetchPriority="high"
            decoding="async"
            className="size-full object-cover"
          />
        </div>

        <header className="relative mx-auto flex w-full max-w-(--breakpoint-2xl) items-center justify-between gap-4 px-4 py-4 sm:px-8">
          <div className="flex min-w-0 items-center gap-2 sm:gap-3">
            <img src="/logo.webp" alt="" width="40" height="40" className="size-8 shrink-0 rounded-tile sm:size-10" />
            <p className="min-w-0 text-sm leading-tight font-semibold sm:text-base">{t('appName')}</p>
          </div>
          <LanguageSwitcher tone="onImage" className="w-36 shrink-0" />
        </header>

        <main className="relative mx-auto flex w-full max-w-(--breakpoint-2xl) flex-1 flex-col justify-center px-4 pt-12 sm:px-8 lg:pt-8">
          <div className="flex max-w-4xl flex-col gap-4">
            <p className="text-sm font-semibold tracking-wider uppercase">{t('home.eyebrow')}</p>
            <h1 className="text-3xl font-bold tracking-tight text-balance md:text-display">{t('home.hero')}</h1>
            <p className="max-w-xl text-base md:text-lg">{t('home.lede')}</p>
          </div>
        </main>
      </div>

      <nav
        aria-label={t('launcher.modules')}
        className="relative mx-auto w-full max-w-(--breakpoint-2xl) px-4 pb-6 sm:px-8 lg:pb-12"
      >
        <ul className="flex flex-wrap justify-center gap-3">
          {navs.map((nav) => {
            const Icon = nav.icon
            return (
              <li
                key={nav.id}
                className={`w-full xs:w-[calc(50%-var(--spacing)*1.5)] md:w-[calc(25%-var(--spacing)*2.25)]
                  wide:w-[calc((100%-var(--spacing)*18)/7)]`}
              >
                <Link
                  to={nav.basePath}
                  className={`group flex h-full min-h-11 flex-wrap items-center gap-3 rounded-card bg-surface-raised p-3
                    text-text shadow-overlay active:scale-98 active:bg-surface-muted motion-reduce:active:scale-100
                    xs:items-start ${focusInsideCard} ${shellTransition}`}
                >
                  <span
                    className={`flex size-10 shrink-0 items-center justify-center rounded-tile bg-primary text-on-primary
                      group-active:bg-primary-pressed ${shellTransition}`}
                  >
                    <Icon size={20} aria-hidden="true" />
                  </span>
                  <span className="min-w-0 grow basis-0 text-sm font-semibold hyphens-none xs:order-last xs:basis-full">
                    {t(nav.nameKey)}
                  </span>
                  <ArrowRight
                    size={18}
                    aria-hidden="true"
                    className={`shrink-0 text-text-muted group-hover:text-text-accent xs:ml-auto ${shellTransition}`}
                  />
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>
    </div>
  )
}
