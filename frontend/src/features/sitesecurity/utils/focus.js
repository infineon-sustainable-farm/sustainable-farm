export const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    '[tabindex]:not([tabindex="-1"])',
].join(",");

/*
 * Where Tab must go to stay inside a dialog, or null to let the browser move
 * focus normally. currentIndex is -1 when focus is not on a focusable element.
 */
export function trappedFocusIndex(currentIndex, count, backwards) {
    if (count === 0) return null;
    const last = count - 1;
    if (currentIndex === -1) return backwards ? last : 0;
    if (backwards && currentIndex === 0) return last;
    if (!backwards && currentIndex === last) return 0;
    return null;
}
