import { CircleAlert } from "lucide-react";
import { cx } from "../../../shared/ui/cx";

/**
 * Error of a request shown above a screen or inside a form, as the machinery form alerts. An
 * optional action (Retry) sits on the right.
 */
function InlineAlert({ children, action, className }) {
  return (
    <div
      role="alert"
      className={cx(
        "flex flex-wrap items-center justify-between gap-3 rounded-control bg-error-tint px-3 py-2 text-sm text-error-fg",
        className
      )}
    >
      <p className="flex items-center gap-2">
        <CircleAlert size={16} aria-hidden="true" className="shrink-0" />
        <span>{children}</span>
      </p>
      {action}
    </div>
  );
}
export default InlineAlert;
