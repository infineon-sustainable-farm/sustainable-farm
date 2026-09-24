import StatusBadge from "./StatusBadge";
import { formatDateTime } from "../utils/format";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const COLUMNS = ["Visitor", "Channel", "Sent at", "Status", "Rating"];

export default function SurveysTable({ surveys }) {
    return (
        <div className="overflow-x-auto">
            <table className="vm-table w-full border-collapse text-sm">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b-2 border-line bg-[#F4F8F8] px-3 py-2.5 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {surveys.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No survey sent yet.
                            </td>
                        </tr>
                    )}
                    {surveys.map((survey) => (
                        <tr key={survey.id} className="border-b border-line">
                            <td className="px-3 py-2.5 font-medium text-ink">
                                {survey.visitorName ?? "—"}
                            </td>
                            <td className="px-3 py-2.5 text-ink">
                                {formatEnumLabel(survey.channel)}
                            </td>
                            <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                {formatDateTime(survey.sentAt)}
                            </td>
                            <td className="px-3 py-2.5">
                                <StatusBadge value={survey.status} />
                            </td>
                            <td className="px-3 py-2.5 text-ink">
                                {survey.rating ? `${survey.rating}★` : "—"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}