import { FIELD_INPUT, FIELD_LABEL, SECTION_CARD } from "./plantsStyles";

/**
 * Block filter. Option values are the raw stored values ("A"); only the label
 * carries the "Block " prefix, so the API always receives the raw value.
 *
 * `label` says which block is filtered: "Block / Plot:" unless a screen filters
 * on another one, such as the destination of a nursery batch.
 *
 * In its own card by default; `bare` drops the card, for a filter placed on the
 * title row of a screen. Its gray-500 label stays above AA either way: #6a7282
 * -> 4.84:1 on white, 4.60:1 on the page. At the keyboard the select shows the
 * 2 px primary ring of every field of the module.
 */
export default function BlockFilter({ blocks, value, onChange, disabled, label = "Block / Plot:", bare = false }) {
    return (
        <div className={`flex flex-wrap items-center gap-3 ${bare ? "" : SECTION_CARD}`}>
            <label htmlFor="plants-block-filter" className={FIELD_LABEL}>
                {label}
            </label>
            <select
                id="plants-block-filter"
                value={value}
                disabled={disabled}
                onChange={(event) => onChange(event.target.value)}
                className={FIELD_INPUT}
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
