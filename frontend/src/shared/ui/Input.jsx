import { forwardRef } from 'react'
import { cx } from './cx'
import { useFieldControl } from './fieldContext'
import { controlBase } from './styles'

/** Text input of the kit (machinery look). Inside a Field it gets its id and aria links. */
const Input = forwardRef(function Input({ className, type = 'text', ...props }, ref) {
  const controlProps = useFieldControl(props)
  return <input ref={ref} type={type} className={cx(controlBase, className)} {...controlProps} />
})

export default Input
