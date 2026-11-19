import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, Input, Select } from "../../../shared/ui";
import { useFertilizers } from "../hooks/useFertilizers";
import { useFertilizerMovementBlocks, useFertilizerMovements } from "../hooks/useFertilizerMovements";
import { formatBlock, formatEnum } from "../utils/format";
import FertilizerMovementsTable from "./FertilizerMovementsTable";
import { SectionLoadError, SectionLoading } from "./FertilizerSectionStatus";

// The movement types the API records through its routes.
const MOVEMENT_TYPES = ["PURCHASE", "APPLICATION", "LOSS"];

/**
 * One filter of the history. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className="shrink-0 text-sm font-medium">
                {label}
            </label>
            <div className="min-w-0 max-w-full">{children}</div>
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
    const { t } = useTranslation("plants");
    // Raw values as the API takes them; an empty string means "no filter".
    const [fertilizerId, setFertilizerId] = useState("");
    const [movementType, setMovementType] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const { data: fertilizers } = useFertilizers();
    const { data: blocks } = useFertilizerMovementBlocks();
    const query = useFertilizerMovements({
        fertilizerId: fertilizerId ? Number(fertilizerId) : null,
        movementType: movementType || null,
        blockCode: blockCode || null,
        from: from || null,
        to: to || null,
    });
    const { data: movements, isPending, isError } = query;
    const isFiltered = Boolean(fertilizerId || movementType || blockCode || from || to);
    const labelledBy = (id) => `movement-history-title ${id}-label`;
    const typeLabel = (type) => formatEnum("movementType", type);

    return (
        <section aria-labelledby="movement-history-title" className="flex flex-col gap-4">
            <h2 id="movement-history-title" className="text-lg font-semibold">
                {t("fertilizer.movements.title")}
            </h2>

            <Card>
                <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
                    <FilterField id="movement-history-fertilizer" label={t("fertilizer.movements.filters.fertilizer")}>
                        <Select
                            id="movement-history-fertilizer"
                            value={fertilizerId}
                            onChange={(event) => setFertilizerId(event.target.value)}
                            aria-labelledby={labelledBy("movement-history-fertilizer")}
                        >
                            <option value="">{t("fertilizer.movements.filters.allFertilizers")}</option>
                            {(fertilizers ?? []).map((fertilizer) => (
                                <option key={fertilizer.id} value={fertilizer.id}>
                                    {fertilizer.name}
                                </option>
                            ))}
                        </Select>
                    </FilterField>

                    <FilterField id="movement-history-type" label={t("fertilizer.movements.filters.type")}>
                        <Select
                            id="movement-history-type"
                            value={movementType}
                            onChange={(event) => setMovementType(event.target.value)}
                            aria-labelledby={labelledBy("movement-history-type")}
                        >
                            <option value="">{t("fertilizer.movements.filters.allTypes")}</option>
                            {MOVEMENT_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {typeLabel(type)}
                                </option>
                            ))}
                        </Select>
                    </FilterField>

                    <FilterField id="movement-history-block" label={t("fertilizer.movements.filters.block")}>
                        <Select
                            id="movement-history-block"
                            value={blockCode}
                            onChange={(event) => setBlockCode(event.target.value)}
                            aria-labelledby={labelledBy("movement-history-block")}
                        >
                            <option value="">{t("fertilizer.movements.filters.allBlocks")}</option>
                            {(blocks ?? []).map((block) => (
                                <option key={block} value={block}>
                                    {formatBlock(block)}
                                </option>
                            ))}
                        </Select>
                    </FilterField>

                    <FilterField id="movement-history-from" label={t("fertilizer.movements.filters.from")}>
                        <Input
                            id="movement-history-from"
                            type="date"
                            value={from}
                            onChange={(event) => setFrom(event.target.value)}
                            aria-labelledby={labelledBy("movement-history-from")}
                        />
                    </FilterField>

                    <FilterField id="movement-history-to" label={t("fertilizer.movements.filters.to")}>
                        <Input
                            id="movement-history-to"
                            type="date"
                            value={to}
                            onChange={(event) => setTo(event.target.value)}
                            aria-labelledby={labelledBy("movement-history-to")}
                        />
                    </FilterField>
                </div>
            </Card>

            {isPending && <SectionLoading>{t("fertilizer.movements.loading")}</SectionLoading>}

            {isError && <SectionLoadError title={t("fertilizer.movements.loadError")} query={query} />}

            {!isPending && !isError && movements.length === 0 && (
                <Card>
                    {isFiltered ? (
                        <EmptyState
                            className="py-8"
                            title={t("fertilizer.movements.noMatch")}
                            message={t("fertilizer.movements.noMatchHint")}
                        />
                    ) : (
                        <EmptyState className="py-8" message={t("fertilizer.movements.empty")} action={recordAction} />
                    )}
                </Card>
            )}

            {!isPending && !isError && movements.length > 0 && <FertilizerMovementsTable movements={movements} />}
        </section>
    );
}
