import { useTranslation } from "react-i18next";
import { Card } from "../../../shared/ui";
import VigorBadge from "./VigorBadge";
import { formatBlock, formatNumber, formatSpacing, formatText } from "../utils/format";

// The keyboard focus mark of the kit: 2 px primary outline, on :focus-visible only.
const FOCUS_RING = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring";

// The expected and actual yields are compared over a period, under this table. The tree count, a
// figure, lines up on the right, header included.
const COLUMNS = ["variety", "block", "trees", "spacing", "vigor"];

/**
 * The variety rows. A row opens the details of its variety, by click or with Enter or Space: the
 * kit Table has no row action of its own, so the table is written here with its classes.
 */
export default function VarietiesTable({ varieties, onSelect }) {
    const { t } = useTranslation("plants");
    return (
        <Card padding="none" className="overflow-x-auto px-5 py-3">
            <table className="w-full border-collapse text-left whitespace-nowrap">
                <caption className="sr-only">{t("varieties.tableCaption")}</caption>
                <thead>
                    <tr className="border-b border-border-divider text-sm text-text-muted">
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={`py-2 pr-4 ${column === "trees" ? "text-right" : ""}`}>
                                {t(`varieties.columns.${column}`)}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {varieties.map((variety) => (
                        <tr
                            key={variety.id}
                            tabIndex={0}
                            onClick={() => onSelect(variety)}
                            onKeyDown={(event) => {
                                if (event.key === "Enter" || event.key === " ") {
                                    event.preventDefault();
                                    onSelect(variety);
                                }
                            }}
                            className={`cursor-pointer border-b border-border-subtle last:border-b-0 hover:bg-surface-muted ${FOCUS_RING}`}
                        >
                            <td className="py-2 pr-4 font-medium">{formatText(variety.name)}</td>
                            <td className="py-2 pr-4">{formatBlock(variety.blockCode)}</td>
                            <td className="py-2 pr-4 text-right">{formatNumber(variety.currentTreeCount)}</td>
                            <td className="py-2 pr-4">{formatSpacing(variety.rowSpacingM, variety.treeSpacingM)}</td>
                            <td className="py-2 pr-4">
                                <VigorBadge value={variety.vigor} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </Card>
    );
}
