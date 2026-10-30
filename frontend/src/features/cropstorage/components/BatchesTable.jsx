import { formatQuantity } from "../utils/formatters";

export default function BatchesTable({ batches }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">Batch code</th>
                        <th className="px-4 py-3 font-semibold">Product</th>
                        <th className="px-4 py-3 font-semibold">Stage</th>
                        <th className="px-4 py-3 font-semibold">Status</th>
                        <th className="px-4 py-3 text-right font-semibold">Current stock</th>
                        <th className="px-4 py-3 font-semibold">Storage zone</th>
                        <th className="px-4 py-3 font-semibold">Expiry</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                    {batches.map((batch) => (
                        <tr key={batch.id ?? batch.batchCode}>
                            <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                                {batch.batchCode ?? batch.batch_code ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {batch.productType ?? batch.product_type ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {batch.productStage ?? batch.product_stage ?? "—"}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{batch.status ?? "—"}</td>
                            <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                                {formatQuantity(
                                    Number(batch.currentQuantityKg ?? batch.current_quantity_kg ?? 0),
                                )}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {batch.storageZone?.name ??
                                    batch.storageZone?.zoneCode ??
                                    batch.storage_zone?.name ??
                                    batch.storage_zone?.zone_code ??
                                    "—"}
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-gray-700">
                                {batch.expiryDate ?? batch.expiry_date ?? "—"}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
