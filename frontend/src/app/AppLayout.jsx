import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

/**
 * Shell of the modules that opted in: the global sidebar and the main area, as MachineryLayout.
 * Under md the menu button sits over the top left corner, so the main area starts below it.
 * The module screens load on first visit (React.lazy): the sidebar stays while one loads.
 *
 * @param {{ navs: object[] }} props - `nav` exports of the integrated modules.
 */
export default function AppLayout({ navs }) {
  return (
    <div className="flex h-dvh">
      <Sidebar navs={navs} />
      <main className="h-dvh min-w-0 flex-1 overflow-y-auto max-md:pt-16">
        <Suspense fallback={null}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
