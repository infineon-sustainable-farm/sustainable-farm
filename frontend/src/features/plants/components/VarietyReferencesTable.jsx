import { Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, Table } from "../../../shared/ui";
import { formatKilograms, formatSeason, formatText } from "../utils/format";

/** The reference of each variety, in the order of the API, each with an Edit button. */
export default function VarietyReferencesTable({ references, onEdit }) {
    const { t } = useTranslation("plants");
    const columns = [
        {
            key: "variety",
            header: t("harvest.references.variety"),
            className: "whitespace-nowrap",
            cell: (reference) => formatText(reference.varietyName),
        },
        {
            key: "yield",
            header: t("harvest.references.yieldPerTree"),
            className: "whitespace-nowrap",
            cell: (reference) => formatKilograms(reference.yieldPerTreeKg),
        },
        {
            key: "season",
            header: t("harvest.references.harvestSeason"),
            className: "whitespace-nowrap",
            cell: (reference) => formatSeason(reference.harvestStartMonth, reference.harvestEndMonth),
        },
        {
            key: "sources",
            header: t("harvest.references.sources"),
            // Allowed to wrap, as in the forecast table: two sources on one line would push the
            // table into a sideways scroll.
            className: "min-w-56",
            cell: (reference) =>
                t("harvest.references.sourcesValue", {
                    yield: formatText(reference.yieldSource),
                    season: formatText(reference.seasonSource),
                }),
        },
    ];

    return (
        <Card>
            <Table
                columns={columns}
                rows={references}
                caption={t("harvest.references.varieties")}
                className="text-sm"
                rowActions={(reference) => (
                    <Button
                        variant="secondary"
                        size="sm"
                        icon={Pencil}
                        onClick={() => onEdit(reference)}
                        aria-label={t("harvest.references.editVariety", { name: reference.varietyName })}
                    >
                        {t("common:actions.edit")}
                    </Button>
                )}
            />
        </Card>
    );
}
