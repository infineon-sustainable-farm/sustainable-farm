import { CircleCheck, CircleX, Info, TriangleAlert, CircleDot } from 'lucide-react'
import Badge from './Badge'

const TONE_ICONS = {
  success: CircleCheck,
  warning: TriangleAlert,
  error: CircleX,
  info: Info,
  neutral: CircleDot,
}

/**
 * State of a record (operational, under maintenance, out of service...), as the machinery
 * StatusBadge ("px-2 py-1"). The translated label always shows; `withIcon` adds the icon of the
 * tone, `icon` replaces it.
 *
 * @param {object} props
 * @param {'success'|'warning'|'error'|'info'|'neutral'} props.tone
 * @param {import('react').ReactNode} props.label - translated state name.
 * @param {boolean} [props.withIcon=false]
 * @param {import('react').ComponentType} [props.icon]
 */
function StatusBadge({ tone = 'neutral', label, withIcon = false, icon, className, ...rest }) {
  const Icon = icon ?? (withIcon ? TONE_ICONS[tone] : undefined)
  return (
    <Badge tone={tone} icon={Icon} size="md" className={className} {...rest}>
      {label}
    </Badge>
  )
}

export default StatusBadge
