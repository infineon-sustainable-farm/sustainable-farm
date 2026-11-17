// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Field from './Field'
import Textarea from './Textarea'

describe('Textarea', () => {
  it('renders a multi-line field with 3 rows and the field look', () => {
    render(<Textarea aria-label="Notes" />)
    const textarea = screen.getByRole('textbox', { name: 'Notes' })
    expect(textarea.tagName).toBe('TEXTAREA')
    expect(textarea).toHaveAttribute('rows', '3')
    expect(textarea).toHaveClass('border-border-input', 'resize-y')
  })

  it('is labelled and described inside a Field', () => {
    render(
      <Field label="Notes" error="Too long.">
        <Textarea rows={5} />
      </Field>,
    )
    const textarea = screen.getByRole('textbox', { name: 'Notes' })
    expect(textarea).toHaveAttribute('rows', '5')
    expect(textarea).toHaveAttribute('aria-invalid', 'true')
    expect(textarea).toHaveAccessibleDescription('Too long.')
  })
})
