import { Outlet } from 'react-router-dom'
import { ToastProvider } from '../shared/ui'

/**
 * Root of every route: provides the kit toasts (useToast) once for the whole application, the
 * launcher, the shell modules and the modules with their own layout alike.
 */
export default function RootLayout() {
  return (
    <ToastProvider>
      <Outlet />
    </ToastProvider>
  )
}
