import { Boxes, Cog, Droplets, LayoutDashboard, Tractor } from 'lucide-react'
import { Outlet } from 'react-router-dom'

// Fake modules for the shell tests, shaped as import.meta.glob results of features/*/routes.jsx.

export const shellModuleNav = {
  id: 'machinery',
  basePath: '/machinery',
  icon: Tractor,
  nameKey: 'machinery:name',
  shell: true,
  items: [
    { to: '/machinery', labelKey: 'machinery:nav.dashboard', icon: LayoutDashboard, end: true },
    { to: '/machinery/equipment-registry', labelKey: 'machinery:nav.equipmentRegistry', icon: Cog },
    { to: '/machinery/spare-parts', labelKey: 'machinery:nav.spareParts', icon: Boxes },
  ],
}

export const otherModuleNav = {
  id: 'watersupply',
  basePath: '/watersupply',
  icon: Droplets,
  nameKey: 'watersupply:name',
  items: [
    { to: '/watersupply', labelKey: 'watersupply:nav.dashboard', icon: LayoutDashboard, end: true },
    { to: '/watersupply/farms', labelKey: 'watersupply:nav.farms', icon: Cog },
  ],
}

export const fakeFiles = {
  '../features/machinery/routes.jsx': {
    default: [
      {
        path: 'machinery',
        children: [
          { index: true, element: <p>machinery home</p> },
          { path: 'equipment-registry', element: <p>equipment screen</p> },
          { path: 'spare-parts', element: <p>spare parts screen</p> },
        ],
      },
    ],
    nav: shellModuleNav,
  },
  '../features/watersupply/routes.jsx': {
    default: [
      {
        path: 'watersupply',
        element: (
          <div data-testid="own-layout">
            <Outlet />
          </div>
        ),
        children: [{ index: true, element: <p>water home</p> }],
      },
    ],
    nav: otherModuleNav,
  },
  '../features/sitesecurity/routes.jsx': {
    default: [{ path: 'sitesecurity', element: <p>security app</p> }],
  },
}

/**
 * The real routes.jsx files, loaded after a matchMedia stub: some module screens read it when
 * their file loads, and jsdom does not provide it.
 */
export async function loadRealModuleFiles() {
  if (!window.matchMedia) {
    window.matchMedia = (query) => ({
      matches: false,
      media: query,
      addEventListener() {},
      removeEventListener() {},
      addListener() {},
      removeListener() {},
    })
  }
  const loaders = import.meta.glob('../features/*/routes.jsx')
  const entries = await Promise.all(Object.entries(loaders).map(async ([path, load]) => [path, await load()]))
  return Object.fromEntries(entries)
}
