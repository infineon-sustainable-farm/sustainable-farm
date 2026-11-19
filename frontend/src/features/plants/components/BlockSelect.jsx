import { OWNER_LINKS } from "../../../app/ownerLinks";
import { OwnedResourceSelect } from "../../../shared/ui";
import { useFarmBlocks } from "../hooks/useFarmBlocks";

/** "A · North block", or the code alone when the field is named after it. */
function blockOptionLabel(block) {
    return !block.name || block.name.trim().toUpperCase() === block.code ? block.code : `${block.code} · ${block.name}`;
}

/**
 * Picks the block a row is written on, among the fields of the farm referential that carry a
 * block code (owner: Farms & fields of Water Supply). The value is the block code, as the plants
 * requests send it. A missing block is added by the owner screen, reached through the hint.
 */
export default function BlockSelect({ id, label, value, onChange, error, hint, farmId = null, required, className }) {
    const { data, isPending, isError, refetch } = useFarmBlocks({ farmId });
    return (
        <OwnedResourceSelect
            id={id}
            label={label}
            value={value}
            onChange={(code) => onChange(code)}
            options={data ?? []}
            getOptionValue={(block) => block.code}
            getOptionLabel={blockOptionLabel}
            loading={isPending}
            loadError={isError}
            onRetry={refetch}
            owner={OWNER_LINKS.field}
            showOwnerLinkAlways
            error={error}
            hint={hint}
            required={required}
            className={className}
        />
    );
}
