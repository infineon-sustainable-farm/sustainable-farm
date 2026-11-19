import { PAGE_SUBTITLE, PAGE_TITLE } from '../styles/ptStyles';

/**
 * Header shared by every Product Transformation list screen.
 *
 * The title block and the action group wrap onto separate lines instead of
 * pushing the page sideways: at 375 px the Compliance screen carries two
 * buttons and used to overflow its container by 50 px. `min-w-0` lets the
 * title block shrink, `flex-wrap` moves the actions under it, and `gap-3`
 * keeps the same rhythm at every breakpoint.
 *
 * The same header is rendered while a list is in its error state, so the page
 * keeps its `<h1>` (and its name for screen readers) when the API fails.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
export default function PTListHeader({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div className="min-w-0">
        <h1 className={PAGE_TITLE}>{title}</h1>
        {subtitle ? <p className={PAGE_SUBTITLE}>{subtitle}</p> : null}
      </div>
      {children ? (
        <div className="flex flex-wrap items-center gap-2 sm:gap-3">{children}</div>
      ) : null}
    </div>
  );
}
