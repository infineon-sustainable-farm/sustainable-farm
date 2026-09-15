import VigorBadge from "./VigorBadge";
import {
    formatBlock,
    formatKilograms,
    formatNumber,
    formatSpacing,
    formatText,
} from "../utils/format";

const COLUMNS = [
    "Variety",
    "Block",
    "Trees",
    "Spacing",
    "Expected Yield",
    "Actual Yield",
    "Vigor",
];

export default function VarietiesTable({ varieties, onSelect }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase"
                            >
                                {column}
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
                            className="cursor-pointer border-b border-gray-100 last:border-b-0 hover:bg-gray-50 focus:bg-gray-50 focus:outline-none"
                        >
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatText(variety.nom)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatBlock(variety.bloc_parcelle)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatNumber(variety.nombre_arbres)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatSpacing(
                                    variety.espacement_inter_rang_m,
                                    variety.espacement_intra_rang_m,
                                )}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatKilograms(variety.rendement_attendu_kg)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatKilograms(variety.rendement_reel_kg)}
                            </td>
                            <td className="px-4 py-3">
                                <VigorBadge value={variety.vigueur} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
