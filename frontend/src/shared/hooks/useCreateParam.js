import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Deep link to the create form of a screen: `<screen>?create=1` opens its "new" dialog on arrival.
 * A screen with several create dialogs uses a value per dialog (`?create=field`, `?create=zone`).
 * Consumer modules link to the owner of a resource this way (see app/ownerLinks.js).
 */
export const CREATE_PARAM = 'create'

/** Value of CREATE_PARAM for the main create dialog of a screen. */
export const CREATE_DEFAULT = '1'

/**
 * URL that opens the create dialog of the screen at `screenPath`.
 *
 * @param {string} screenPath - absolute path of the owner screen, e.g. '/machinery/equipment-registry'.
 * @param {string} [value] - which dialog, CREATE_DEFAULT by default.
 * @returns {string}
 */
export function ownerCreatePath(screenPath, value = CREATE_DEFAULT) {
  const [pathname, query = ''] = screenPath.split('?')
  const params = new URLSearchParams(query)
  params.set(CREATE_PARAM, value)
  return `${pathname}?${params.toString()}`
}

/**
 * Reads the create deep link of the current screen.
 *
 * The screen opens its dialog while `open` is true (`open={isFormOpen || open}`) and calls
 * `clear()` when the dialog closes (cancel, Escape or save): the parameter leaves the URL, with a
 * history replace, so going back does not open the dialog again. Other parameters are kept.
 *
 * @param {string} [value] - which dialog this call reads, CREATE_DEFAULT by default.
 * @returns {[boolean, () => void]} [open, clear]
 */
export function useCreateParam(value = CREATE_DEFAULT) {
  const [searchParams, setSearchParams] = useSearchParams()
  const open = searchParams.get(CREATE_PARAM) === value

  const clear = useCallback(() => {
    setSearchParams(
      (current) => {
        if (!current.has(CREATE_PARAM)) return current
        const next = new URLSearchParams(current)
        next.delete(CREATE_PARAM)
        return next
      },
      { replace: true },
    )
  }, [setSearchParams])

  return [open, clear]
}
