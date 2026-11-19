import { useEffect, useId, useRef, useState } from "react";
import { Bell, Trash2, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton } from "../../../shared/ui";
import { useLowStockSpareParts } from "../hooks/useLowStockSpareParts";

const STORAGE_KEY = "machinery.dismissed-low-stock";

// Sidebar link look of the shell (app/Sidebar.jsx): white text on primary, hover on primary-hover,
// white focus outline inside the button.
const TOGGLE_CLASS = [
  "flex min-h-9 w-full cursor-pointer items-center gap-3 rounded-control px-3 py-2 text-sm text-on-primary",
  "hover:bg-primary-hover pointer-coarse:min-h-11",
  "focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-on-primary",
  "transition duration-(--duration-fast) ease-standard motion-reduce:transition-none",
].join(" ");

function readDismissedQuantities() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    const parsed = stored ? JSON.parse(stored) : {};
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

/**
 * Low stock popover of the machinery sidebar footer (nav.sidebarFooter): a Notifications button
 * with the count of parts at or below their reorder threshold, and a panel listing them, each
 * dismissible until its quantity changes.
 */
function LowStockNotifications() {
  const { t } = useTranslation("machinery");
  const { data } = useLowStockSpareParts();
  const [dismissed, setDismissed] = useState(readDismissedQuantities);
  const [panelState, setPanelState] = useState(null);
  const containerRef = useRef(null);
  const panelId = useId();

  const lowStockParts = data ?? [];
  const notifications = lowStockParts.filter(
    (sparePart) => dismissed[sparePart.id] !== sparePart.quantity
  );

  // Opens automatically the first time the user reaches machinery management,
  // as long as there are notifications left. Once the user closes it, it stays closed.
  const isOpen = panelState ?? notifications.length > 0;

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(dismissed));
  }, [dismissed]);

  useEffect(() => {
    if (!isOpen) return;
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setPanelState(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  function dismissNotification(sparePart) {
    setDismissed((previous) => ({ ...previous, [sparePart.id]: sparePart.quantity }));
  }

  function dismissAll() {
    setDismissed((previous) => {
      const next = { ...previous };
      notifications.forEach((sparePart) => {
        next[sparePart.id] = sparePart.quantity;
      });
      return next;
    });
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setPanelState(!isOpen)}
        className={TOGGLE_CLASS}
        title={t("notifications.title")}
        aria-expanded={isOpen}
        aria-controls={isOpen ? panelId : undefined}
      >
        <Bell size={18} aria-hidden="true" className="shrink-0" />
        {t("notifications.button")}
        {notifications.length > 0 && (
          <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-pill bg-danger px-1 text-xs font-bold text-on-primary">
            {notifications.length}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          id={panelId}
          className="absolute bottom-full left-0 z-(--z-overlay) mb-2 max-h-80 w-72 overflow-y-auto rounded-control border border-border-divider bg-surface-raised text-text shadow-overlay"
        >
          <div className="flex items-center justify-between gap-2 border-b border-border-subtle px-4 py-3">
            <h2 className="text-sm font-bold">{t("notifications.title")}</h2>
            {notifications.length > 0 && (
              <button
                type="button"
                onClick={dismissAll}
                className="cursor-pointer rounded-control text-xs text-text-muted hover:text-text focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring pointer-coarse:min-h-11"
              >
                {t("notifications.deleteAll")}
              </button>
            )}
          </div>

          {notifications.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-text-muted">{t("notifications.empty")}</p>
          ) : (
            <ul>
              {notifications.map((sparePart) => (
                <li
                  key={sparePart.id}
                  className="flex items-start gap-3 border-b border-border-subtle px-4 py-3 last:border-b-0"
                >
                  <TriangleAlert size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-warning-fg" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium break-words">{sparePart.name}</p>
                    <p className="text-xs text-text-muted">
                      {t("notifications.item", {
                        quantity: sparePart.quantity,
                        threshold: sparePart.reorderThreshold,
                      })}
                    </p>
                  </div>
                  <IconButton
                    icon={Trash2}
                    label={t("notifications.deleteOne")}
                    tone="neutral"
                    onClick={() => dismissNotification(sparePart)}
                    className="-my-1.5 -mr-2"
                  />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
export default LowStockNotifications;
