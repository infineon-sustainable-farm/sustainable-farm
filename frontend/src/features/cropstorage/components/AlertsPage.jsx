import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { EmptyState, Field, PageHeader, Select } from "../../../shared/ui";
import { acknowledgeAlert, fetchAlerts, resolveAlert } from "../api";
import { GROUP, ALERT_STATUSES, enumLabel } from "../utils/enums";
import { usePageTitle } from "../utils/usePageTitle";
import AlertsTable from "./AlertsTable";
import FormMessage from "./FormMessage";
import QueryState from "./QueryState";

export default function AlertsPage() {
    const { t } = useTranslation("cropstorage");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const queryClient = useQueryClient();
    usePageTitle("alerts.title");

    const alertsQuery = useQuery({ queryKey: ["cropstorage", "alerts"], queryFn: fetchAlerts });

    const actionMutation = useMutation({
        mutationFn: ({ id, action }) => (action === "acknowledge" ? acknowledgeAlert(id) : resolveAlert(id)),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cropstorage"] }),
    });

    const visibleAlerts = (alertsQuery.data ?? []).filter(
        (alert) => statusFilter === "ALL" || String(alert.status ?? "").toUpperCase() === statusFilter,
    );

    return (
        <div className="flex flex-col gap-6">
            <PageHeader title={t("alerts.title")} subtitle={t("alerts.subtitle")} />
            <Field label={t("alerts.filter")} className="max-w-xs">
                <Select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                    <option value="ALL">{t("alerts.allStatuses")}</option>
                    {ALERT_STATUSES.map((status) => (
                        <option key={status} value={status}>
                            {enumLabel(t, GROUP.alertStatus, status)}
                        </option>
                    ))}
                </Select>
            </Field>

            <FormMessage
                error={actionMutation.isError ? t("alerts.updateError", { message: actionMutation.error.message }) : null}
                success={
                    actionMutation.isSuccess
                        ? t(actionMutation.variables.action === "resolve" ? "alerts.resolved" : "alerts.acknowledged")
                        : null
                }
            />

            <QueryState query={alertsQuery} errorKey="alerts.loadError" />
            {alertsQuery.isSuccess && visibleAlerts.length === 0 && (
                <EmptyState
                    message={
                        statusFilter === "ALL"
                            ? t("alerts.empty")
                            : t("alerts.emptyFiltered", { status: enumLabel(t, GROUP.alertStatus, statusFilter) })
                    }
                />
            )}
            {alertsQuery.isSuccess && visibleAlerts.length > 0 && (
                <AlertsTable
                    alerts={visibleAlerts}
                    isUpdating={actionMutation.isPending}
                    onAcknowledge={(id) => actionMutation.mutate({ id, action: "acknowledge" })}
                    onResolve={(id) => actionMutation.mutate({ id, action: "resolve" })}
                />
            )}
        </div>
    );
}
