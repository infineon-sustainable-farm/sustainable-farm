// @vitest-environment jsdom
import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Skeleton from './Skeleton'

describe('Skeleton', () => {
  it('is a decorative pulsing block that stops under reduced motion', () => {
    const { container } = render(<Skeleton className="h-8 w-52" />)
    const block = container.firstChild
    expect(block).toHaveAttribute('aria-hidden', 'true')
    expect(block).toHaveClass('animate-pulse', 'motion-reduce:animate-none', 'bg-neutral-bg', 'rounded-control', 'h-8', 'w-52')
  })

  it('takes the radius of what it stands for', () => {
    const { container } = render(<Skeleton radius="card" />)
    expect(container.firstChild).toHaveClass('rounded-card')
  })
})
