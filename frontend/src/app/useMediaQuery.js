import { useCallback, useSyncExternalStore } from 'react'

/** Whether the media query matches; false where matchMedia does not exist (tests, server). */
export function useMediaQuery(query) {
  const subscribe = useCallback(
    (onChange) => {
      if (typeof window === 'undefined' || !window.matchMedia) return () => {}
      const list = window.matchMedia(query)
      list.addEventListener('change', onChange)
      return () => list.removeEventListener('change', onChange)
    },
    [query],
  )
  const getSnapshot = () =>
    typeof window !== 'undefined' && Boolean(window.matchMedia) && window.matchMedia(query).matches
  return useSyncExternalStore(subscribe, getSnapshot, () => false)
}
