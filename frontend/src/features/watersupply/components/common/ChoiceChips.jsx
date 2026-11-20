const CHIP = [
  'inline-flex min-h-8 items-center rounded-pill border px-3 py-1.5 text-xs font-semibold',
  'pointer-coarse:min-h-11',
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring',
  'transition duration-(--duration-fast) ease-standard motion-reduce:transition-none',
].join(' ')
const ON = 'border-primary bg-primary text-on-primary'
const OFF = 'border-border-input bg-surface-raised text-text hover:bg-surface-muted'

/**
 * Row of toggle chips (period, view switcher): one choice pressed at a time.
 *
 * @param {{ label: string, options: { value: string, label: string }[], value: string,
 *   onChange: (value: string) => void }} props - `label` names the group for assistive technologies.
 */
export function ChoiceChips({ label, options, value, onChange }) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap gap-2">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          aria-pressed={option.value === value}
          onClick={() => onChange(option.value)}
          className={`${CHIP} ${option.value === value ? ON : OFF}`}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
