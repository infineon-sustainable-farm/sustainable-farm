/**
 * Block filter. Option values are the raw stored values ("A"); only the label
 * carries the "Block " prefix, so the API always receives the raw value.
 *
 * `label` says which block is filtered: "Block / Plot:" unless a screen filters
 * on another one, such as the destination of a nursery batch.
 */
export default function BlockFilter({ blocks, value, onChange, disabled, label = "Block / Plot:" }) {
    return (
        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
            <label
                htmlFor="plants-block-filter"
                className="text-xs font-semibold text-gray-500"
            >
                {label}
            </label>
            <select
                id="plants-block-filter"
                value={value}
                disabled={disabled}
                onChange={(event) => onChange(event.target.value)}
                className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:text-gray-400"
            >
                <option value="">All blocks</option>
                {blocks.map((block) => (
                    <option key={block} value={block}>
                        {`Block ${block}`}
                    </option>
                ))}
            </select>
        </div>
    );
}
