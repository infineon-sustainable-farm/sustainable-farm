import { useId } from 'react'
import { CircleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import Button from './Button'
import Field from './Field'
import OwnerHint from './OwnerHint'
import Select from './Select'

const defaultOptionValue = (option) => option.id
const defaultOptionLabel = (option) => option.name ?? option.label ?? String(option.id)

/**
 * Picks a resource that another module owns (equipment, person, farm, field, zone, variety, lot):
 * a kit Field and Select filled by the caller from the owner's API. When there is nothing to
 * choose, a hint sends the user to the owner module, with a link that opens its create form;
 * `showOwnerLinkAlways` keeps a discreet link under a filled list too.
 *
 * The caller fetches (React Query or useEffect) and passes `options`, `loading`, `loadError` and
 * `onRetry`. The value is the id of the option (`getOptionValue`), as a string in the DOM.
 *
 * @param {object} props
 * @param {import('react').ReactNode} props.label
 * @param {string | number | null} [props.value] - id of the chosen option, '' or null for none.
 * @param {(value: string, option: object | null) => void} [props.onChange] - id and the option.
 * @param {object[]} [props.options] - resources loaded from the owner's API.
 * @param {(option: object) => string | number} [props.getOptionValue] - `option.id` by default.
 * @param {(option: object) => string} [props.getOptionLabel] - `option.name` by default.
 * @param {string} [props.placeholder] - first empty option, `common:owner.choose` by default.
 * @param {boolean} [props.loading] - the list is loading: the select waits, disabled.
 * @param {import('react').ReactNode} [props.loadError] - the list failed to load: message shown.
 * @param {() => void} [props.onRetry] - retry button of the load error.
 * @param {{ moduleId: string, createPath: string }} [props.owner] - owner module (app/ownerLinks.js).
 * @param {boolean} [props.showOwnerLinkAlways] - link to the owner under a filled list too.
 * @param {import('react').ReactNode} [props.hint] - help text of the field.
 * @param {import('react').ReactNode} [props.error] - validation error of the field.
 * @param {boolean} [props.required]
 * @param {boolean} [props.disabled]
 * @param {string} [props.id] - id of the select (generated otherwise).
 * @param {string} [props.name]
 */
function OwnedResourceSelect({
  label,
  value,
  onChange,
  options = [],
  getOptionValue = defaultOptionValue,
  getOptionLabel = defaultOptionLabel,
  placeholder,
  loading = false,
  loadError,
  onRetry,
  owner,
  showOwnerLinkAlways = false,
  hint,
  error,
  required,
  disabled,
  id,
  name,
  className,
}) {
  const { t } = useTranslation('common')
  const generatedId = useId()
  const controlId = id ?? `owned-${generatedId}`
  const ownerHintId = `${controlId}-owner`
  const loadErrorId = `${controlId}-load-error`

  const failed = !loading && Boolean(loadError)
  const empty = !loading && !failed && options.length === 0
  const ownerVariant = owner && !loading && !failed ? (empty ? 'empty' : showOwnerLinkAlways ? 'more' : null) : null
  const describedBy = [ownerVariant && ownerHintId, failed && loadErrorId].filter(Boolean).join(' ') || undefined

  const choices = options.map((option) => ({
    value: String(getOptionValue(option)),
    label: getOptionLabel(option),
  }))

  function handleChange(event) {
    const next = event.target.value
    const option = options.find((candidate) => String(getOptionValue(candidate)) === next) ?? null
    onChange?.(next, option)
  }

  let firstLabel = placeholder ?? t('owner.choose')
  if (loading) firstLabel = t('status.loading')
  else if (failed || empty) firstLabel = t('owner.none')

  return (
    <div className={className}>
      <Field label={label} hint={hint} error={error} id={controlId} required={required}>
        <Select
          name={name}
          value={loading || failed || empty ? '' : (value ?? '')}
          onChange={handleChange}
          disabled={disabled || loading || failed || empty}
          aria-busy={loading || undefined}
          aria-describedby={describedBy}
        >
          <option value="">{firstLabel}</option>
          {!loading &&
            choices.map((choice) => (
              <option key={choice.value} value={choice.value}>
                {choice.label}
              </option>
            ))}
        </Select>
      </Field>

      {failed && (
        <div className="mt-1 flex flex-wrap items-center gap-2">
          <p id={loadErrorId} role="alert" className="flex items-center gap-1 text-xs text-error-fg">
            <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
            <span>{loadError === true ? t('owner.loadError') : loadError}</span>
          </p>
          {onRetry && (
            <Button variant="secondary" size="sm" onClick={onRetry}>
              {t('actions.retry')}
            </Button>
          )}
        </div>
      )}

      {ownerVariant && <OwnerHint owner={owner} variant={ownerVariant} id={ownerHintId} className="mt-1" />}
    </div>
  )
}

export default OwnedResourceSelect
