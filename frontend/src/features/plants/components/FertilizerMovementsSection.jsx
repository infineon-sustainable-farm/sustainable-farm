import { useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useFertilizers } from "../hooks/useFertilizers";
import { useFertilizerMovementBlocks, useFertilizerMovements } from "../hooks/useFertilizerMovements";
import { formatBlock, formatCode } from "../utils/format";
import FertilizerMovementsTable from "./FertilizerMovementsTable";
import PlantsEmptyState from "./PlantsEmptyState";

// The movement types the API records through its routes.
const MOVEMENT_TYPES = ["PURCHASE", "APPLICATION", "LOSS"];

const LABEL_CLASS = "text-xs font-semibold text-gray-500";
const CONTROL_CLASS =
    "min-w-0 max-w-full rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none";

/**
 * One filter of the history. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className={`${LABEL_CLASS} shrink-0`}>
                {label}
            </label>
            {children}
        </div>
    );
}

/**
 * Purchases, applications and losses, filtered by fertilizer, type, block and
 * dates, both included. The block filter keeps applications only, as the API
 * records no block for a purchase or a loss; its options are the blocks the
 * recorded applications name. Filters the API cannot match give an empty list.
 *
 * `recordAction` is the page's "Record a movement" button, offered again when
 * no movement is recorded at all.
 */
export default function FertilizerMovementsSection({ recordAction }) {
    // Raw values as the API takes them; an empty string means "no filter".
    const [fertilizerId, setFertilizerId] = useState("");
    const [movementType, setMovementType] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const { data: fertilizers } = useFertilizers();
    const { data: blocks } = useFertilizerMovementBlocks();
    const { data: movements, isPending, isError, refetch, isFetching } = useFertilizerMovements({
        fertilizerId: fertilizerId ? Number(fertilizerId) : null,
        movementType: movementType || null,
        blockCode: blockCode || null,
        from: from || null,
        to: to || null,
    });
    const isFiltered = Boolean(fertilizerId || movementType || blockCode || from || to);
    const labelledBy = (id) => `movement-history-title ${id}-label`;

    return (
        <section aria-labelledby="movement-history-title" className="flex flex-col gap-4">
            <h3 id="movement-history-title" className="font-heading text-base font-bold text-gray-900">
                Movement history
            </h3>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
                <FilterField id="movement-history-fertilizer" label="Fertilizer">
                    <select
                        id="movement-history-fertilizer"
                        value={fertilizerId}
                        onChange={(event) => setFertilizerId(event.target.value)}
                        aria-labelledby={labelledBy("movement-history-fertilizer")}
                        className={CONTROL_CLASS}
                    >
                        <option value="">All fertilizers</option>
                        {(fertilizers ?? []).map((fertilizer) => (
                            <option key={fertilizer.id} value={fertilizer.id}>
                                {fertilizer.name}
                            </option>
                        ))}
                    </select>
                </FilterField>

                <FilterField id="movement-history-type" label="Type">
                    <select
                        id="movement-history-type"
                        value={movementType}
                        onChange={(event) => setMovementType(event.target.value)}
                        aria-labelledby={labelledBy("movement-history-type")}
                        className={CONTROL_CLASS}
                    >
                        <option value="">All types</option>
                        {MOVEMENT_TYPES.map((type) => (
                            <option key={type} value={type}>
                                {formatCode(type)}
                            </option>
                        ))}
                    </select>
                </FilterField>

                <FilterField id="movement-history-block" label="Block (applications only)">
                    <select
                        id="movement-history-block"
                        value={blockCode}
                        onChange={(event) => setBlockCode(event.target.value)}
                        aria-labelledby={labelledBy("movement-history-block")}
                        className={CONTROL_CLASS}
                    >
                        <option value="">All blocks</option>
                        {(blocks ?? []).map((block) => (
                            <option key={block} value={block}>
                                {formatBlock(block)}
                            </option>
                        ))}
                    </select>
                </FilterField>

                <FilterField id="movement-history-from" label="From">
                    <input
                        id="movement-history-from"
                        type="date"
                        value={from}
                        onChange={(event) => setFrom(event.target.value)}
                        aria-labelledby={labelledBy("movement-history-from")}
                        className={CONTROL_CLASS}
                    />
                </FilterField>

                <FilterField id="movement-history-to" label="To">
                    <input
                        id="movement-history-to"
                        type="date"
                        value={to}
                        onChange={(event) => setTo(event.target.value)}
                        aria-labelledby={labelledBy("movement-history-to")}
                        className={CONTROL_CLASS}
                    />
                </FilterField>
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading movements…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Movements could not be loaded
                        </p>
                        {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1; gray-500 gave 4.32:1. */}
                        <p className="mt-1 text-sm text-gray-600">
                            The request to the server did not succeed.
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && movements.length === 0 && (
                isFiltered ? (
                    <PlantsEmptyState
                        title="No movement matches these filters"
                        hint="Try another fertilizer, type, block or date range."
                    />
                ) : (
                    <PlantsEmptyState title="No movement recorded yet" action={recordAction} />
                )
            )}

            {!isPending && !isError && movements.length > 0 && <FertilizerMovementsTable movements={movements} />}
        </section>
    );
}
