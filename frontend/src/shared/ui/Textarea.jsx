import { forwardRef } from 'react'
import { cx } from './cx'
import { useFieldControl } from './fieldContext'
import { controlBase } from './styles'

/** Multi-line text field of the kit (machinery look), 3 rows by default. */
const Textarea = forwardRef(function Textarea({ className, rows = 3, ...props }, ref) {
  const controlProps = useFieldControl(props)
  return <textarea ref={ref} rows={rows} className={cx(controlBase, 'resize-y', className)} {...controlProps} />
})

export default Textarea
