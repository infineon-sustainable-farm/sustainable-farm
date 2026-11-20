import { OwnedResourceSelect } from '../shared/ui'
import { usePersons } from '../shared/hooks/usePersons'
import { OWNER_LINKS } from './ownerLinks'

const personLabel = (person) => person.fullName

/**
 * Picks a person of the farm people directory (owner: visitor management Staff screen) for a
 * field of another screen: operator, technician, observer, applicator... The list comes from
 * GET /api/staff/persons; when the person is missing, a link sends the user to the Staff screen.
 *
 * `currentName` keeps the person a record already references in the list when the directory no
 * longer lists it (deactivated), so editing the record does not lose it.
 *
 * @param {object} props
 * @param {string | number | null} [props.value] - person id, '' or null for none.
 * @param {(value: string, person: object | null) => void} [props.onChange]
 * @param {string} [props.currentName] - name shown by the record for `value`.
 * Other props go to OwnedResourceSelect (label, error, required, placeholder, hint, name, id...).
 */
function PersonSelect({ value, currentName, showOwnerLinkAlways = false, ...props }) {
  const persons = usePersons()
  let options = persons.data ?? []
  const hasValue = value !== null && value !== undefined && value !== ''
  if (hasValue && !persons.isPending && !persons.isError
      && !options.some((person) => String(person.id) === String(value))) {
    options = [...options, { id: value, fullName: currentName || `#${value}` }]
  }

  return (
    <OwnedResourceSelect
      {...props}
      value={hasValue ? String(value) : ''}
      options={options}
      getOptionLabel={personLabel}
      loading={persons.isPending}
      loadError={persons.isError}
      onRetry={persons.refetch}
      owner={OWNER_LINKS.person}
      showOwnerLinkAlways={showOwnerLinkAlways}
    />
  )
}

export default PersonSelect
