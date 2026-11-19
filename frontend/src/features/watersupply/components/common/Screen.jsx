import { PageHeader } from '../../../../shared/ui'

/**
 * Frame of a water supply screen, as the machinery screens: 24 px padding, the kit page header
 * (title, subtitle, actions) and the content under it.
 */
export function Screen({ title, subtitle, actions, children }) {
  return (
    <div className="p-6">
      <PageHeader className="mb-6" title={title} subtitle={subtitle} actions={actions} />
      <div className="flex flex-col gap-6">{children}</div>
    </div>
  )
}

/** List on the left (three fifths), form on the right, stacked under lg. */
export function SplitLayout({ main, aside }) {
  return (
    <div className="grid items-start gap-6 lg:grid-cols-5">
      <div className="flex min-w-0 flex-col gap-6 lg:col-span-3">{main}</div>
      <div className="flex min-w-0 flex-col gap-6 lg:col-span-2">{aside}</div>
    </div>
  )
}

/** Row of list filters (selects, search) above a table. */
export function Filters({ children }) {
  return <div className="mb-4 flex flex-wrap items-end gap-3">{children}</div>
}

/** Buttons at the end of a form. */
export function FormActions({ children }) {
  return <div className="flex flex-wrap justify-end gap-2">{children}</div>
}
