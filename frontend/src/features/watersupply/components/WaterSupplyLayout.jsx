import { useCallback, useMemo } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useToast } from '../../../shared/ui'
import { viewUrl } from '../paths'

/**
 * Route element of the module, inside the application shell (global sidebar from app/AppLayout,
 * kit toasts from app/RootLayout). It renders no chrome of its own: it hands the active screen its
 * two module props through the Outlet context: `notify` shows a confirmation as a kit toast,
 * `onNavigate` opens another water supply screen by its view id (dashboard shortcuts). The screens are wrapped by withModuleUi (ModuleScreen.jsx).
 */
export default function WaterSupplyLayout() {
  const navigate = useNavigate()
  const toast = useToast()

  const notify = useCallback((message, tone = 'success') => toast.show({ message, tone }), [toast])
  const goToView = useCallback((viewId) => navigate(viewUrl(viewId)), [navigate])
  const moduleUi = useMemo(() => ({ notify, onNavigate: goToView }), [notify, goToView])

  return <Outlet context={moduleUi} />
}
