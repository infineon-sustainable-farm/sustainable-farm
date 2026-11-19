import { cx } from './cx'

// Machinery loading blocks: "bg-gray-100 animate-pulse" with the radius of what they stand for.
const RADII = { control: 'rounded-control', tile: 'rounded-tile', card: 'rounded-card', pill: 'rounded-pill' }

/**
 * Gray block standing for content that loads. Decoration only (aria-hidden): the container says
 * "Loading" to screen readers (Table does). The pulse stops under reduced motion.
 *
 * @param {object} props
 * @param {'control'|'tile'|'card'|'pill'} [props.radius='control']
 * @param {string} [props.className] - size of the block (h-8 w-52, h-40...).
 */
function Skeleton({ radius = 'control', className }) {
  return (
    <span
      aria-hidden="true"
      className={cx('block animate-pulse bg-neutral-bg motion-reduce:animate-none', RADII[radius] ?? RADII.control, className)}
    />
  )
}

export default Skeleton
