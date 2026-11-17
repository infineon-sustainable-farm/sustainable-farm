import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { Menu, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, useLocation } from 'react-router-dom'
import LanguageSwitcher from './LanguageSwitcher'
import { focusOnPrimary, shellTransition } from './styles'
import { useMediaQuery } from './useMediaQuery'

const ICON_SIZE = 18
const DESKTOP_QUERY = '(min-width: 48rem)'
const FOCUSABLE = 'a[href], button:not([disabled]), select:not([disabled])'

// Machinery sidebar links: 36 px, rounded-lg, white text; the current link on primary-active,
// hover on primary-hover. min-h instead of h so that a long German or French label wraps instead
// of being cut; 44 px on touch screens.
const LINK_BASE = [
  'flex min-h-9 w-full items-center gap-3 rounded-control px-3 py-2 text-sm text-on-primary',
  'pointer-coarse:min-h-11',
  focusOnPrimary,
  shellTransition,
].join(' ')

const itemClass = ({ isActive }) =>
  `${LINK_BASE} ${isActive ? 'bg-primary-active' : 'hover:bg-primary-hover'}`

function isWithin(pathname, basePath) {
  return pathname === basePath || pathname.startsWith(`${basePath}/`)
}

/**
 * Global sidebar, machinery's rendering for every module: logo back to the launcher, one group
 * per integrated module (the current one expanded with its links, the others as a link to their
 * home), the language switcher at the bottom.
 *
 * Under md it is a drawer behind a menu button, as the plants sidebar: overlay, closes on a link,
 * on Escape and on the overlay, focus moved into it on open and back to the button on close,
 * Tab kept inside while open, links out of the tab order while closed.
 *
 * @param {{ navs: object[] }} props - `nav` exports of the integrated modules.
 */
export default function Sidebar({ navs }) {
  const { t } = useTranslation('common')
  const { pathname } = useLocation()
  const isDesktop = useMediaQuery(DESKTOP_QUERY)
  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)
  const navRef = useRef(null)
  const toggleRef = useRef(null)
  const navId = useId()
  const activeNav = navs.find((nav) => isWithin(pathname, nav.basePath))

  const closeAndRefocus = useCallback(() => {
    setOpen(false)
    toggleRef.current?.focus()
  }, [])

  // A link was followed: the drawer closes and focus goes with the new page.
  const closeOnNavigate = () => setOpen(false)

  useEffect(() => {
    if (!open || isDesktop) return undefined
    navRef.current?.querySelector(FOCUSABLE)?.focus()

    function onKeyDown(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        closeAndRefocus()
        return
      }
      if (event.key !== 'Tab') return
      const focusable = [...(containerRef.current?.querySelectorAll(FOCUSABLE) ?? [])]
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, isDesktop, closeAndRefocus])

  const drawerHidden = !isDesktop && !open
  const ToggleIcon = open ? X : Menu

  return (
    <div ref={containerRef} className="contents">
      <button
        ref={toggleRef}
        type="button"
        aria-label={open ? t('shell.closeMenu') : t('shell.openMenu')}
        aria-expanded={open}
        aria-controls={navId}
        onClick={() => setOpen((value) => !value)}
        className={`fixed top-4 left-4 z-(--z-overlay) rounded-control bg-primary p-2 text-on-primary md:hidden pointer-coarse:p-2.5 ${focusOnPrimary}`}
      >
        <ToggleIcon aria-hidden="true" />
      </button>

      {open && (
        <div
          aria-hidden="true"
          data-testid="sidebar-overlay"
          onClick={closeAndRefocus}
          className="fixed inset-0 z-(--z-drawer) bg-backdrop md:hidden"
        />
      )}

      <nav
        ref={navRef}
        id={navId}
        aria-label={t('shell.navigation')}
        inert={drawerHidden}
        className={`fixed top-0 left-0 z-(--z-drawer) flex h-dvh w-60 shrink-0 flex-col bg-primary px-3 pt-5 text-on-primary
          transition-transform duration-(--duration-slow) ease-standard motion-reduce:transition-none
          md:static md:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <Link
          to="/"
          onClick={closeOnNavigate}
          className={`flex flex-col items-center gap-2 rounded-control px-2 py-1 ${focusOnPrimary}`}
        >
          <img src="/logo.webp" alt="" className="h-auto w-20" />
          <span className="text-center text-xs font-semibold tracking-widest uppercase">{t('appName')}</span>
        </Link>

        <ul className="mt-6 flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
          {navs.map((nav) => {
            const expanded = nav === activeNav
            const ModuleIcon = nav.icon
            return (
              <li key={nav.id}>
                <Link
                  to={nav.basePath}
                  onClick={closeOnNavigate}
                  className={`${LINK_BASE} hover:bg-primary-hover ${expanded ? 'font-semibold' : ''}`}
                >
                  <ModuleIcon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
                  {t(nav.nameKey)}
                </Link>
                {expanded && (
                  <ul className="mt-1 flex flex-col gap-1 pl-3">
                    {nav.items.map((item) => {
                      const ItemIcon = item.icon
                      return (
                        <li key={item.to}>
                          <NavLink to={item.to} end={item.end} onClick={closeOnNavigate} className={itemClass}>
                            <ItemIcon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
                            {t(item.labelKey)}
                          </NavLink>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </li>
            )
          })}
        </ul>

        <LanguageSwitcher className="mt-auto pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]" />
      </nav>
    </div>
  )
}
