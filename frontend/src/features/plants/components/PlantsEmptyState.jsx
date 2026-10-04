/**
 * Empty state of the module's screens: a title, and a hint or an action on
 * what to do next.
 *
 * Local to the module, and used by both screens so they read the same. The
 * shared EmptyState takes a single message plus an optional action, with no
 * title and no secondary line, so it cannot render this; changing it is a
 * decision for the team that owns it.
 */
export default function PlantsEmptyState({ title, hint, action }) {
    return (
        <div className="rounded-xl border border-gray-200 bg-white px-6 py-16 text-center">
            <p className="font-heading text-base font-bold text-gray-900">{title}</p>
            {hint && <p className="mt-1 text-sm text-gray-500">{hint}</p>}
            {action && <div className="mt-4 flex justify-center">{action}</div>}
        </div>
    );
}
