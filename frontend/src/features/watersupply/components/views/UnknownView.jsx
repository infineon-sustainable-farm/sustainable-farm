import { EmptyState } from './EmptyState'

/**
 * Unknown route inside the module (/watersupply/xxx): we tell the user instead of
 * silently redirecting to the dashboard, which used to hide mistyped URLs.
 */
export function UnknownView() {
  return (
    <EmptyState
      title="Screen not found"
      description="This water supply screen does not exist. Pick one from the menu on the left."
    />
  )
}
