import { useEffect, useRef, useState } from "react";
import { Bell, Trash2, TriangleAlert } from "lucide-react";
import { useLowStockSpareParts } from "../hooks/useLowStockSpareParts";

const STORAGE_KEY = "machinery.dismissed-low-stock";

function readDismissedQuantities() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    const parsed = stored ? JSON.parse(stored) : {};
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function LowStockNotifications() {
  const { data } = useLowStockSpareParts();
  const [dismissed, setDismissed] = useState(readDismissedQuantities);
  const [panelState, setPanelState] = useState(null);
  const containerRef = useRef(null);

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
        className="relative p-2 rounded-lg text-gray-500 hover:bg-gray-100"
        title="Low stock notifications"
      >
        <Bell size={20} />
        {notifications.length > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-4 h-4 px-1 rounded-full bg-red-600 text-white text-[10px] font-bold flex items-center justify-center">
            {notifications.length}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 max-h-96 overflow-y-auto bg-white border border-gray-200 rounded-lg shadow-lg z-50">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h2 className="text-sm font-bold">Low stock notifications</h2>
            {notifications.length > 0 && (
              <button type="button" onClick={dismissAll} className="text-xs text-gray-500 hover:text-gray-700">
                Delete all
              </button>
            )}
          </div>

          {notifications.length === 0 ? (
            <p className="px-4 py-6 text-sm text-gray-500 text-center">No low stock notifications.</p>
          ) : (
            <ul>
              {notifications.map((sparePart) => (
                <li
                  key={sparePart.id}
                  className="flex items-start gap-3 px-4 py-3 border-b border-gray-50 last:border-b-0"
                >
                  <TriangleAlert size={16} className="mt-0.5 shrink-0 text-amber-500" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-800 break-words">{sparePart.name}</p>
                    <p className="text-xs text-gray-500">
                      {sparePart.quantity} in stock — reorder threshold {sparePart.reorderThreshold}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => dismissNotification(sparePart)}
                    className="text-gray-400 hover:text-red-600"
                    title="Delete notification"
                  >
                    <Trash2 size={16} />
                  </button>
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
