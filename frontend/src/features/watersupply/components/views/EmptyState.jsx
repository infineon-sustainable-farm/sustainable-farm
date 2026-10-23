/**
 * Empty state of the watersupply module: title + explanation, styled by the module's
 * design system (ws-* classes).
 *
 * Why an internal component: the views show here a title AND an explanation, whereas
 * the shared component (frontend/src/shared/components/EmptyState.jsx) exposes a single
 * message styled with Tailwind classes. Both usages therefore coexist, without the module
 * modifying a shared file.
 *
 * Name kept (`EmptyState`) so that only the import paths change in the views.
 */
export function EmptyState({ title, description }) {
  return (
    <div className="ws-empty">
      <p className="ws-empty-title">{title}</p>
      {description ? <p className="ws-empty-text">{description}</p> : null}
    </div>
  )
}
