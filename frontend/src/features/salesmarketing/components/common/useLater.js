import { useCallback, useEffect, useRef } from 'react';

/**
 * later(fn, ms) runs fn after ms, like window.setTimeout, and the pending calls are cancelled when
 * the screen closes: a message that hides itself does not update a screen that is gone.
 */
export function useLater() {
  const timers = useRef(new Set());
  useEffect(() => {
    const pending = timers.current;
    return () => {
      pending.forEach((id) => window.clearTimeout(id));
      pending.clear();
    };
  }, []);
  return useCallback((fn, ms) => {
    const id = window.setTimeout(() => {
      timers.current.delete(id);
      fn();
    }, ms);
    timers.current.add(id);
  }, []);
}
