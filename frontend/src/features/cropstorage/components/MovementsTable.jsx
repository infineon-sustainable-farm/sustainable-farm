const numberFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 });

function zoneName(zone) {
    return zone?.name ?? zone?.zoneCode ?? zone?.zone_code ?? "—";
}

export default function MovementsTable({ movements }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">ID</th>
                        <th className="px-4 py-3 font-semibold">Movement type</th>
                        <th className="px-4 py-3 text-right font-semibold">Quantity (kg)</th>
                        <th className="px-4 py-3 font-semibold">From zone</th>
                        <th className="px-4 py-3 font-semibold">To zone</th>
                        <th className="px-4 py-3 font-semibold">Reason</th>
                        <th className="px-4 py-3 font-semibold">Moved at</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                    {movements.map((movement) => (
                        <tr key={movement.id}>
                            <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                                {movement.id}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {movement.movementType ?? movement.type ?? "—"}
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                                {numberFormat.format(Number(movement.quantityKg ?? movement.quantity_kg ?? 0))}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{zoneName(movement.fromZone ?? movement.from_zone)}</td>
                            <td className="px-4 py-3 text-gray-700">{zoneName(movement.toZone ?? movement.to_zone)}</td>
                            <td className="px-4 py-3 text-gray-700">{movement.reason || "—"}</td>
                            <td className="whitespace-nowrap px-4 py-3 text-gray-700">
                                {movement.movedAt ?? movement.moved_at ?? "—"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
