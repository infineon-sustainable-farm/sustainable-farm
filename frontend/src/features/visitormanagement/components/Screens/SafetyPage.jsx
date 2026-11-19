import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Modal, PageHeader, Table } from "../../../../shared/ui";
import { useRegistrations } from "../../hooks/useRegistrations";
import { useDeliverBriefing } from "../../hooks/useSafety";
import { useVisitorFormat } from "../../utils/format";
import BriefingForm from "../Forms/BriefingForm";
import {
    BriefingActions,
    BriefingSignature,
    BriefingStaff,
    BriefingStatus,
    BriefingTime,
} from "../BriefingCells";
import { LoadError, LoadingBlock } from "../LoadState";

/*
 * Only confirmed visitors can receive a briefing (approving a registration creates it), so the
 * tracker lists CONFIRMED and CHECKED_IN registrations that carry a briefing. Registrations still
 * pending approval are handled on the Registration screen.
 */
const TRACKED_STATUSES = new Set(["CONFIRMED", "CHECKED_IN"]);

export default function SafetyPage() {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const [recording, setRecording] = useState(null);

    useEffect(() => {
        document.title = t("safety.documentTitle");
    }, [t]);

    const { data: registrations, isPending, isError, refetch, isFetching } = useRegistrations();
    const deliverMutation = useDeliverBriefing();

    const tracked = (registrations ?? []).filter(
        (registration) => registration.briefingId && TRACKED_STATUSES.has(registration.status),
    );

    const pendingId = deliverMutation.isPending ? deliverMutation.variables?.registrationId : null;
    const deliverError =
        deliverMutation.isError && deliverMutation.variables
            ? {
                  registrationId: deliverMutation.variables.registrationId,
                  message: deliverMutation.error.message,
              }
            : null;

    const columns = [
        { key: "visitor", header: t("safety.columns.visitor"), cell: (row) => row.visitorName ?? format.none },
        { key: "group", header: t("safety.columns.group"), cell: (row) => row.groupSize ?? format.none },
        {
            key: "time",
            header: t("safety.columns.time"),
            className: "whitespace-nowrap",
            cell: (row) => <BriefingTime registration={row} />,
        },
        { key: "staff", header: t("safety.columns.deliveredBy"), cell: (row) => <BriefingStaff registration={row} /> },
        { key: "signature", header: t("safety.columns.signature"), cell: (row) => <BriefingSignature registration={row} /> },
        { key: "status", header: t("safety.columns.status"), cell: (row) => <BriefingStatus registration={row} /> },
    ];

    return (
        <div className="flex flex-col gap-6 p-6">
            <PageHeader title={t("safety.title")} />

            <p className="rounded-tile bg-primary-tint px-4 py-3 text-sm text-text">
                <span className="font-semibold">{t("safety.processLabel")}</span> {t("safety.processText")}
            </p>

            {isPending && <LoadingBlock label={t("safety.loading")} />}

            {isError && (
                <LoadError title={t("safety.loadError")} onRetry={() => refetch()} retrying={isFetching} />
            )}

            {!isPending && !isError && (
                <div>
                    <p className="mb-4 text-sm font-semibold text-text">
                        {t("safety.tracked", { count: tracked.length })}
                    </p>

                    <Table
                        caption={t("safety.title")}
                        columns={columns}
                        rows={tracked}
                        emptyMessage={t("safety.empty")}
                        rowActions={(registration) => (
                            <BriefingActions
                                registration={registration}
                                isDelivering={pendingId === registration.id}
                                deliverError={
                                    deliverError?.registrationId === registration.id
                                        ? deliverError.message
                                        : null
                                }
                                onRecord={setRecording}
                            />
                        )}
                    />
                    <p className="mt-6 text-xs text-text-muted">{t("safety.footnote")}</p>
                </div>
            )}

            <Modal
                open={Boolean(recording)}
                onClose={() => setRecording(null)}
                title={t("safety.form.title", { name: recording?.visitorName ?? "" })}
                size="xl"
            >
                {recording && (
                    <BriefingForm
                        registrationId={recording.id}
                        isSubmitting={deliverMutation.isPending}
                        submitError={deliverMutation.error?.message}
                        serverFieldErrors={deliverMutation.error?.data?.fieldErrors}
                        onSubmit={(data) =>
                            deliverMutation.mutate(
                                { registrationId: recording.id, data },
                                { onSuccess: () => setRecording(null) },
                            )
                        }
                        onCancel={() => setRecording(null)}
                    />
                )}
            </Modal>
        </div>
    );
}
