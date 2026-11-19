import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import { ToastProvider } from '../shared/ui'

/**
 * Root of every route: provides the kit toasts (useToast) once for the whole application, the
 * launcher, the shell modules and the modules with their own layout alike, and the boundary of the
 * screens loaded on first visit (React.lazy) outside the shell.
 */
export default function RootLayout() {
  return (
    <ToastProvider>
      <Suspense fallback={null}>
        <Outlet />
      </Suspense>
    </ToastProvider>
  )
}
