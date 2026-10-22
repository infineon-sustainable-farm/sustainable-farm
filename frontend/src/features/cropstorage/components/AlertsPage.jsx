import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, TriangleAlert } from "lucide-react";
import { acknowledgeAlert, fetchAlerts, resolveAlert } from "../api";
import AlertsTable from "./AlertsTable";

const STATUS_FILTERS = ["ALL", "ACTIVE", "ACKNOWLEDGED", "RESOLVED"];

function normalizeStatus(status) {
    return String(status ?? "").toUpperCase();
}

export default function AlertsPage() {
    const [statusFilter, setStatusFilter] = useState("ALL");
    const queryClient = useQueryClient();

    useEffect(() => {
        document.title = "Alerts — Crop Storage";
    }, []);

    const alertsQuery = useQuery({
        queryKey: ["cropstorage", "alerts"],
        queryFn: fetchAlerts,
    });

    const actionMutation = useMutation({
        mutationFn: ({ id, action }) =>
            action === "acknowledge" ? acknowledgeAlert(id) : resolveAlert(id),
        onSuccess: () =>
            queryClient.invalidateQueries({ queryKey: ["cropstorage", "alerts"] }),
    });

    const visibleAlerts = (alertsQuery.data ?? []).filter(
        (alert) => statusFilter === "ALL" || normalizeStatus(alert.status) === statusFilter,
    );

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">Alerts</h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Review storage alerts and update their status.
                    </p>
                </header>

                <div>
                    <label
                        htmlFor="alert-status-filter"
                        className="mb-1.5 block text-xs tracking-wide text-primary uppercase"
                    >
                        Status
                    </label>
                    <select
                        id="alert-status-filter"
                        value={statusFilter}
                        onChange={(event) => setStatusFilter(event.target.value)}
                        className="w-full max-w-xs rounded-md border border-line bg-white px-2.5 py-2 text-sm"
                    >
                        {STATUS_FILTERS.map((status) => (
                            <option key={status} value={status}>
                                {status === "ALL" ? "All statuses" : status}
                            </option>
                        ))}
                    </select>
                </div>

                {actionMutation.isError && (
                    <div
                        role="alert"
                        className="flex items-center gap-3 rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error"
                    >
                        <TriangleAlert size={20} />
                        Alert could not be updated: {actionMutation.error.message}
                        <button
                            type="button"
                            onClick={() => actionMutation.reset()}
                            className="ml-auto font-semibold underline"
                        >
                            Dismiss
                        </button>
                    </div>
                )}

                {alertsQuery.isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-12 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading alerts…
                    </div>
                )}
                {alertsQuery.isError && (
                    <div
                        role="alert"
                        className="flex items-center gap-3 rounded-xl border border-error/30 bg-error/5 p-4 text-sm text-error"
                    >
                        <TriangleAlert size={20} />
                        Alerts could not be loaded: {alertsQuery.error.message}
                        <button
                            type="button"
                            onClick={() => alertsQuery.refetch()}
                            className="ml-auto font-semibold underline"
                        >
                            Retry
                        </button>
                    </div>
                )}
                {!alertsQuery.isPending && !alertsQuery.isError && visibleAlerts.length === 0 && (
                    <div className="rounded-xl border border-dashed border-gray-300 bg-white px-6 py-10 text-center text-sm text-gray-600">
                        {statusFilter === "ALL"
                            ? "No alerts recorded yet."
                            : `No ${statusFilter.toLowerCase()} alerts.`}
                    </div>
                )}
                {!alertsQuery.isPending && !alertsQuery.isError && visibleAlerts.length > 0 && (
                    <AlertsTable
                        alerts={visibleAlerts}
                        isUpdating={actionMutation.isPending}
                        onAcknowledge={(id) => actionMutation.mutate({ id, action: "acknowledge" })}
                        onResolve={(id) => actionMutation.mutate({ id, action: "resolve" })}
                    />
                )}
            </div>
        </div>
    );
}
