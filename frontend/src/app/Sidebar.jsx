import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { LayoutGrid, Menu, X } from 'lucide-react'
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

// Section heading of the current module's links: small, semibold, uppercase, in the sidebar
// colours. It stays on-primary (a lighter tint would drop under 4.5:1 on primary).
const SECTION_HEADING = 'px-3 pt-3 pb-1 text-xs font-semibold tracking-wider text-on-primary uppercase'

function isWithin(pathname, basePath) {
  return pathname === basePath || pathname.startsWith(`${basePath}/`)
}

/**
 * Splits the items of a module into runs of consecutive items sharing the same `sectionKey`
 * (undefined for the items outside any section), in their order.
 *
 * @param {object[]} items
 * @returns {{ sectionKey?: string, items: object[] }[]}
 */
function groupBySection(items) {
  const groups = []
  for (const item of items) {
    const last = groups[groups.length - 1]
    if (last && last.sectionKey === item.sectionKey) last.items.push(item)
    else groups.push({ sectionKey: item.sectionKey, items: [item] })
  }
  return groups
}

function NavItem({ item, onNavigate }) {
  const ItemIcon = item.icon
  return (
    <li>
      <NavLink to={item.to} end={item.end} onClick={onNavigate} className={itemClass}>
        <ItemIcon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
        {item.label}
      </NavLink>
    </li>
  )
}

/**
 * Links of the current module. Items without `sectionKey` are listed flat; a run of items with
 * the same `sectionKey` becomes a group labelled by its heading (role group + aria-labelledby).
 */
function ModuleItems({ items, onNavigate }) {
  const { t } = useTranslation('common')
  const baseId = useId()
  return (
    <ul className="flex flex-col gap-1">
      {groupBySection(items).map((group, index) => {
        const links = group.items.map((item) => (
          <NavItem key={item.to} item={{ ...item, label: t(item.labelKey) }} onNavigate={onNavigate} />
        ))
        if (!group.sectionKey) return links
        const headingId = `${baseId}-section-${index}`
        return (
          <li key={`${group.sectionKey}-${index}`} role="group" aria-labelledby={headingId}>
            <p id={headingId} className={SECTION_HEADING}>
              {t(group.sectionKey)}
            </p>
            <ul className="flex flex-col gap-1">{links}</ul>
          </li>
        )
      })}
    </ul>
  )
}

/**
 * Sidebar of the current module, machinery's rendering for every module: logo back to the
 * launcher with the module name under it, the module's own links (and section headings), a link
 * back to all modules, the module's `sidebarFooter` (if any) and the language switcher at the
 * bottom. Each module has its own sidebar: the other modules are reached from the launcher.
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
  const ModuleFooter = activeNav?.sidebarFooter

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
          <span className="text-center text-xs font-semibold tracking-widest uppercase">
            {activeNav ? t(activeNav.nameKey) : t('appName')}
          </span>
        </Link>

        <div className="mt-6 flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
          {activeNav && <ModuleItems items={activeNav.items} onNavigate={closeOnNavigate} />}
          <Link
            to="/"
            onClick={closeOnNavigate}
            className={`${LINK_BASE} mt-4 hover:bg-primary-hover`}
          >
            <LayoutGrid size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
            {t('shell.allModules')}
          </Link>
        </div>

        <div className="mt-auto flex flex-col gap-4 pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
          {ModuleFooter && (
            <div data-testid="sidebar-module-footer">
              <ModuleFooter />
            </div>
          )}
          <LanguageSwitcher />
        </div>
      </nav>
    </div>
  )
}
