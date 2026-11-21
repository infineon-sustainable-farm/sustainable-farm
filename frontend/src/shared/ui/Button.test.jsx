// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { Plus } from 'lucide-react'
import { createRef } from 'react'
import { describe, expect, it, vi } from 'vitest'
import Button from './Button'

describe('Button', () => {
  it('renders a button of type button with its label and calls onClick', () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Save</Button>)
    const button = screen.getByRole('button', { name: 'Save' })
    expect(button).toHaveAttribute('type', 'button')
    fireEvent.click(button)
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('uses the machinery primary look by default and the token classes of each variant', () => {
    const { rerender } = render(<Button>Go</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-primary', 'text-on-primary', 'rounded-control', 'px-4', 'py-2')
    rerender(<Button variant="secondary">Go</Button>)
    expect(screen.getByRole('button')).toHaveClass('border', 'border-border-input')
    rerender(<Button variant="danger">Go</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-danger')
    rerender(<Button variant="ghost">Go</Button>)
    expect(screen.getByRole('button')).toHaveClass('text-text-accent')
  })

  it('shows the focus mark on keyboard focus only and scales when pressed', () => {
    render(<Button>Go</Button>)
    const button = screen.getByRole('button')
    expect(button.className).toContain('focus-visible:outline-2')
    expect(button.className).not.toMatch(/(^|\s)focus:ring/)
    expect(button.className).toContain('enabled:active:scale-98')
    expect(button.className).not.toContain('hover:scale')
  })

  it('keeps its label on one line, except at full width where the container sets the width', () => {
    const { rerender } = render(<Button>Ajouter un équipement</Button>)
    expect(screen.getByRole('button')).toHaveClass('whitespace-nowrap')
    rerender(<Button fullWidth>Télécharger le manifeste</Button>)
    expect(screen.getByRole('button')).toHaveClass('w-full')
    expect(screen.getByRole('button')).not.toHaveClass('whitespace-nowrap')
  })

  it('renders a decorative leading icon', () => {
    const { container } = render(<Button icon={Plus}>Add</Button>)
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.getByRole('button', { name: 'Add' })).toBeInTheDocument()
  })

  it('does not fire when disabled', () => {
    const onClick = vi.fn()
    render(
      <Button disabled onClick={onClick}>
        Save
      </Button>,
    )
    const button = screen.getByRole('button', { name: 'Save' })
    expect(button).toBeDisabled()
    fireEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('is busy and blocked while loading, with a spinner', () => {
    const onClick = vi.fn()
    const { container } = render(
      <Button loading onClick={onClick}>
        Save
      </Button>,
    )
    const button = screen.getByRole('button', { name: 'Save' })
    expect(button).toHaveAttribute('aria-busy', 'true')
    expect(button).toBeDisabled()
    expect(container.querySelector('svg')).toHaveClass('animate-spin', 'motion-reduce:animate-none')
    fireEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('accepts type submit and forwards its ref', () => {
    const ref = createRef()
    render(
      <Button type="submit" ref={ref}>
        Send
      </Button>,
    )
    expect(screen.getByRole('button')).toHaveAttribute('type', 'submit')
    expect(ref.current).toBe(screen.getByRole('button'))
  })
})
