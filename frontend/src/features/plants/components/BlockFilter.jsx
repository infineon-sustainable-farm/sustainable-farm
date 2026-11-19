import { useTranslation } from "react-i18next";
import { Card, Select } from "../../../shared/ui";
import { formatBlock } from "../utils/format";

/**
 * Block filter. Option values are the raw stored values ("A"); only the label
 * carries the "Block " prefix, so the API always receives the raw value.
 *
 * `label` says which block is filtered: "Block / Plot:" unless a screen filters
 * on another one, such as the destination of a nursery batch.
 *
 * In its own card by default; `bare` drops the card, for a filter placed on the
 * title row of a screen.
 */
export default function BlockFilter({ blocks, value, onChange, disabled, label, bare = false }) {
    const { t } = useTranslation("plants");
    const filter = (
        <div className="flex flex-wrap items-center gap-3">
            <label htmlFor="plants-block-filter" className="text-sm font-medium">
                {label ?? t("blockFilter.label")}
            </label>
            <div>
                <Select
                    id="plants-block-filter"
                    value={value}
                    disabled={disabled}
                    onChange={(event) => onChange(event.target.value)}
                >
                    <option value="">{t("blockFilter.all")}</option>
                    {blocks.map((block) => (
                        <option key={block} value={block}>
                            {formatBlock(block)}
                        </option>
                    ))}
                </Select>
            </div>
        </div>
    );
    return bare ? filter : <Card>{filter}</Card>;
}
