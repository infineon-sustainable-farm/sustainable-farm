import { useState } from 'react';
import { ListFilter } from 'lucide-react';
import { Button, IconButton } from '../../../../shared/ui';

/**
 * Button that opens a short list of filter choices under it. Choosing closes the list.
 *
 * @param {object} props
 * @param {string} props.label - button text, or accessible name when iconOnly.
 * @param {boolean} [props.iconOnly]
 * @param {boolean} [props.highlighted] - marks the button while a non-default filter applies.
 * @param {string} [props.heading] - small title above the choices.
 * @param {{ value: string, label: string }[]} props.options
 * @param {string} props.value
 * @param {(value: string) => void} props.onChange
 * @param {'left'|'right'} [props.align='right']
 */
function FilterMenu({ label, iconOnly = false, highlighted = false, heading, options, value, onChange, align = 'right' }) {
  const [open, setOpen] = useState(false);
  const active = open || highlighted;
  return (
    <div className="relative shrink-0">
      {iconOnly ? (
        <IconButton
          icon={ListFilter}
          size="md"
          tone={active ? 'primary' : 'neutral'}
          label={label}
          aria-expanded={open}
          className={active ? 'bg-primary-tint' : undefined}
          onClick={() => setOpen((current) => !current)}
        />
      ) : (
        <Button
          variant="secondary"
          icon={ListFilter}
          aria-expanded={open}
          className={active ? 'bg-primary-tint text-text-accent' : undefined}
          onClick={() => setOpen((current) => !current)}
        >
          {label}
        </Button>
      )}
      {open && (
        <div
          className={`absolute top-full z-(--z-overlay) mt-2 w-48 rounded-control border border-border-subtle bg-surface-raised p-1 shadow-overlay ${
            align === 'left' ? 'left-0' : 'right-0'
          }`}
        >
          {heading && <p className="px-2 py-1 text-xs font-semibold text-text-muted">{heading}</p>}
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              aria-pressed={value === option.value}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className={`w-full cursor-pointer rounded-control px-2 py-2 text-left text-sm hover:bg-surface-muted ${
                value === option.value ? 'bg-primary-tint font-semibold text-text-accent' : 'text-text'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default FilterMenu;
