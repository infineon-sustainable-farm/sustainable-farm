function displayValue(value) {
    return value ?? "—";
}

function alertStatus(alert) {
    return String(alert.status ?? "").toUpperCase();
}

function sensorLabel(sensor) {
    return sensor?.name ?? sensor?.sensorCode ?? sensor?.sensor_code ?? "—";
}

function raisedAt(alert) {
    return alert.raisedAt ?? alert.createdAt ?? alert.raised_at ?? alert.created_at ?? "—";
}

export default function AlertsTable({ alerts, isUpdating, onAcknowledge, onResolve }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                <thead className="bg-gray-50 text-xs tracking-wide text-gray-500 uppercase">
                    <tr>
                        <th className="px-4 py-3 font-semibold">ID</th>
                        <th className="px-4 py-3 font-semibold">Alert type</th>
                        <th className="px-4 py-3 font-semibold">Severity</th>
                        <th className="px-4 py-3 font-semibold">Message</th>
                        <th className="px-4 py-3 font-semibold">Sensor</th>
                        <th className="px-4 py-3 font-semibold">Raised at</th>
                        <th className="px-4 py-3 font-semibold">Status</th>
                        <th className="px-4 py-3 font-semibold">Actions</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                    {alerts.map((alert) => {
                        const status = alertStatus(alert);
                        return (
                            <tr key={alert.id}>
                                <td className="whitespace-nowrap px-4 py-3 font-medium text-gray-900">
                                    {alert.id}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {displayValue(alert.alertType ?? alert.type ?? alert.alert_type)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">{displayValue(alert.severity)}</td>
                                <td className="min-w-48 px-4 py-3 text-gray-700">
                                    {displayValue(alert.message)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {sensorLabel(alert.sensor ?? alert.sensorRef)}
                                </td>
                                <td className="whitespace-nowrap px-4 py-3 text-gray-700">
                                    {raisedAt(alert)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">{displayValue(alert.status)}</td>
                                <td className="whitespace-nowrap px-4 py-3">
                                    <div className="flex gap-2">
                                        {status === "ACTIVE" && (
                                            <button
                                                type="button"
                                                onClick={() => onAcknowledge(alert.id)}
                                                disabled={isUpdating}
                                                className="rounded-md border border-line px-2.5 py-1.5 text-xs font-medium text-primary hover:bg-primary-50 disabled:opacity-60"
                                            >
                                                Acknowledge
                                            </button>
                                        )}
                                        {(status === "ACTIVE" || status === "ACKNOWLEDGED") && (
                                            <button
                                                type="button"
                                                onClick={() => onResolve(alert.id)}
                                                disabled={isUpdating}
                                                className="rounded-md bg-primary px-2.5 py-1.5 text-xs font-medium text-white hover:bg-primary-dark disabled:opacity-60"
                                            >
                                                Resolve
                                            </button>
                                        )}
                                        {status === "RESOLVED" && (
                                            <span className="text-xs text-gray-400">No actions</span>
                                        )}
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
