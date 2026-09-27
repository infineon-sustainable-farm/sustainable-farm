/**
 * Etat vide du module watersupply : titre + explication, habille par le design system du
 * module (classes ws-*).
 *
 * Pourquoi un composant interne : les vues affichent ici un titre ET une explication, alors
 * que le composant partage (frontend/src/shared/components/EmptyState.jsx) expose un message
 * unique en classes Tailwind. Les deux usages coexistent donc, sans que le module modifie
 * un fichier partage.
 *
 * Nom conserve (`EmptyState`) pour que seuls les chemins d'import changent dans les vues.
 */
export function EmptyState({ title, description }) {
  return (
    <div className="ws-empty">
      <p className="ws-empty-title">{title}</p>
      {description ? <p className="ws-empty-text">{description}</p> : null}
    </div>
  )
}
