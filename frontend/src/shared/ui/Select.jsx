import { forwardRef } from 'react'
import { cx } from './cx'
import { useFieldControl } from './fieldContext'
import { controlBase } from './styles'

/**
 * Native select of the kit (machinery look). Pass <option> children, or `options` as
 * [{ value, label }] with translated labels.
 */
const Select = forwardRef(function Select({ className, options, children, ...props }, ref) {
  const controlProps = useFieldControl(props)
  return (
    <select ref={ref} className={cx(controlBase, 'cursor-pointer', className)} {...controlProps}>
      {options
        ? options.map((option) => (
            <option key={option.value} value={option.value} disabled={option.disabled}>
              {option.label}
            </option>
          ))
        : children}
    </select>
  )
})

export default Select
