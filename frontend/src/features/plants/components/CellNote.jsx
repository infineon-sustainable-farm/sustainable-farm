/**
 * A second line under a value in a table cell, in small muted text: what only
 * documents the value above it (a source, a scientific name, an amount in the
 * other currency), so the table keeps few columns.
 */
export default function CellNote({ children, className = "" }) {
    return <span className={`block text-xs font-normal text-text-muted ${className}`}>{children}</span>;
}
