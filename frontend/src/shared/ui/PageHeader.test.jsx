// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import PageHeader from './PageHeader'

describe('PageHeader', () => {
  it('renders the title as the h1 of the page', () => {
    render(<PageHeader title="Dashboard" />)
    expect(screen.getByRole('heading', { level: 1, name: 'Dashboard' })).toHaveClass('text-2xl', 'font-bold')
  })

  it('renders the subtitle, meta text and actions', () => {
    render(
      <PageHeader
        title="Equipment"
        subtitle="All the assets"
        meta="Monday"
        actions={<button type="button">Add equipment</button>}
      />,
    )
    expect(screen.getByText('All the assets')).toHaveClass('text-sm', 'text-text-muted')
    expect(screen.getByText('Monday')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add equipment' })).toBeInTheDocument()
  })
})
