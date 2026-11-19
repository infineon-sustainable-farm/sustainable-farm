import { NO_VALUE } from "../utils/format";

/**
 * Renders a formatted value, giving the "no value" dash one single colour across the module: the
 * muted text token, whatever the cell or the dialog around it. Anything other than the dash is
 * passed through untouched.
 */
export default function Value({ children }) {
    return children === NO_VALUE ? <span className="text-text-muted">{NO_VALUE}</span> : children;
}
