const numberFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 });

export default function StorageZonesTable({ zones }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">Code</th>
                        <th className="px-4 py-3 font-semibold">Name</th>
                        <th className="px-4 py-3 font-semibold">Type</th>
                        <th className="px-4 py-3 text-right font-semibold">Capacity (kg)</th>
                        <th className="px-4 py-3 text-right font-semibold">Capacity (m³)</th>
                        <th className="px-4 py-3 font-semibold">Used</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                    {zones.map((zone) => {
                        const percent = zone.usedPercent;
                        return (
                            <tr key={zone.id ?? zone.zoneCode ?? zone.code}>
                                <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                                    {zone.zoneCode ?? zone.code ?? "—"}
                                </td>
                                <td className="px-4 py-3 text-gray-700">{zone.name || "—"}</td>
                                <td className="px-4 py-3 text-gray-700">
                                    {zone.type ?? zone.zoneType ?? "—"}
                                </td>
                                <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                                    {numberFormat.format(zone.capacityKg)}
                                </td>
                                <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                                    {numberFormat.format(zone.capacityM3)}
                                </td>
                                <td className="min-w-40 px-4 py-3">
                                    <div className="flex items-center gap-3">
                                        <div
                                            className="h-2.5 min-w-20 flex-1 overflow-hidden rounded-full bg-gray-100"
                                            role="progressbar"
                                            aria-label={`${zone.zoneCode ?? zone.code ?? zone.name} capacity used`}
                                            aria-valuemin={0}
                                            aria-valuemax={100}
                                            aria-valuenow={Math.min(percent, 100)}
                                            aria-valuetext={`${numberFormat.format(percent)}% used`}
                                        >
                                            <div
                                                className={`h-full rounded-full ${
                                                    percent > 100 ? "bg-error" : "bg-primary"
                                                }`}
                                                style={{ width: `${Math.min(percent, 100)}%` }}
                                            />
                                        </div>
                                        <span className="w-14 text-right tabular-nums text-gray-600">
                                            {numberFormat.format(percent)}%
                                        </span>
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
