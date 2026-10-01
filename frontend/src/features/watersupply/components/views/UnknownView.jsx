import { EmptyState } from './EmptyState'

/**
 * Route inconnue a l'interieur du module (/watersupply/xxx) : on informe au lieu de
 * rediriger silencieusement vers le dashboard, ce qui masquait les URL mal saisies.
 */
export function UnknownView() {
  return (
    <EmptyState
      title="Screen not found"
      description="This water supply screen does not exist. Pick one from the menu on the left."
    />
  )
}
