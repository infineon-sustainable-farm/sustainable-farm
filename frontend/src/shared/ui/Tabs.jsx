import { useId, useRef, useState } from 'react'
import { cx } from './cx'
import { focusRing, transition } from './styles'

// Machinery has no tabs. Built on its tokens and without an underline (no separator lines): a
// muted track, the selected tab raised on a white surface with a shadow and in bold, so the state
// is not carried by colour only.

/**
 * Tabs with the WAI-ARIA tabs pattern: one tab stop (roving tabindex), Left and Right arrows move
 * and select, Home and End go to the ends.
 *
 * @param {object} props
 * @param {Array<{ id: string, label: import('react').ReactNode, icon?: import('react').ComponentType,
 *   content?: import('react').ReactNode, disabled?: boolean }>} props.tabs
 * @param {string} [props.value] - selected tab id (controlled).
 * @param {string} [props.defaultValue] - first selected tab when uncontrolled.
 * @param {(id: string) => void} [props.onChange]
 * @param {string} props.label - translated name of the tab list.
 */
function Tabs({ tabs, value, defaultValue, onChange, label, className }) {
  const baseId = useId()
  const enabledTabs = tabs.filter((tab) => !tab.disabled)
  const [localValue, setLocalValue] = useState(defaultValue ?? enabledTabs[0]?.id)
  const selected = value ?? localValue
  const tabRefs = useRef(new Map())

  const tabId = (id) => `${baseId}-tab-${id}`
  const panelId = (id) => `${baseId}-panel-${id}`

  function select(id) {
    if (value === undefined) setLocalValue(id)
    onChange?.(id)
  }

  function onKeyDown(event) {
    const index = enabledTabs.findIndex((tab) => tab.id === selected)
    let next
    if (event.key === 'ArrowRight') next = enabledTabs[(index + 1) % enabledTabs.length]
    else if (event.key === 'ArrowLeft') next = enabledTabs[(index - 1 + enabledTabs.length) % enabledTabs.length]
    else if (event.key === 'Home') next = enabledTabs[0]
    else if (event.key === 'End') next = enabledTabs[enabledTabs.length - 1]
    if (!next) return
    event.preventDefault()
    select(next.id)
    tabRefs.current.get(next.id)?.focus()
  }

  const selectedTab = tabs.find((tab) => tab.id === selected)

  return (
    <div className={className}>
      <div
        role="tablist"
        aria-label={label}
        onKeyDown={onKeyDown}
        className="inline-flex max-w-full flex-wrap gap-1 rounded-control bg-surface-muted p-1"
      >
        {tabs.map(({ id, label: tabLabel, icon: Icon, disabled, content }) => {
          const isSelected = id === selected
          return (
            <button
              key={id}
              ref={(node) => {
                if (node) tabRefs.current.set(id, node)
                else tabRefs.current.delete(id)
              }}
              type="button"
              role="tab"
              id={tabId(id)}
              aria-selected={isSelected}
              aria-controls={content !== undefined ? panelId(id) : undefined}
              tabIndex={isSelected ? 0 : -1}
              disabled={disabled}
              onClick={() => select(id)}
              className={cx(
                'inline-flex min-h-9 cursor-pointer items-center gap-2 whitespace-nowrap rounded-control px-3 text-sm pointer-coarse:min-h-11',
                'disabled:cursor-not-allowed disabled:opacity-60',
                isSelected
                  ? 'bg-surface-raised font-semibold text-text shadow-raised'
                  : 'font-medium text-text-muted enabled:hover:text-text',
                focusRing,
                transition,
              )}
            >
              {Icon && <Icon size={16} aria-hidden="true" />}
              {tabLabel}
            </button>
          )
        })}
      </div>
      {selectedTab?.content !== undefined && (
        <div role="tabpanel" id={panelId(selectedTab.id)} aria-labelledby={tabId(selectedTab.id)} tabIndex={0} className={cx('mt-4 rounded-control', focusRing)}>
          {selectedTab.content}
        </div>
      )}
    </div>
  )
}

export default Tabs
