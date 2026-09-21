import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useRegistrations } from "../../hooks/useRegistrations";
import { useActiveStaff, useDeliverBriefing } from "../../hooks/useSafety";
import Modal from "../Modal";
import BriefingForm from "../Forms/BriefingForm";
import BriefingRow from "../BriefingRow";

const COLUMNS = [
    "Visitor",
    "Group",
    "Briefing time",
    "Delivered by",
    "Signature",
    "Status",
    "Actions",
];

/*
 * Only confirmed visitors can receive a briefing (approving a registration
 * creates it), so the tracker lists CONFIRMED and CHECKED_IN registrations
 * that carry a briefing. Registrations still pending approval are handled on
 * the Registration screen.
 */
const TRACKED_STATUSES = new Set(["CONFIRMED", "CHECKED_IN"]);

export default function SafetyPage() {
    const [recording, setRecording] = useState(null);

    useEffect(() => {
        document.title = "Safety Briefing Tracker — Visitor Management";
    }, []);

    const {
        data: registrations,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useRegistrations();
    const { data: staff } = useActiveStaff();
    const deliverMutation = useDeliverBriefing();

    const tracked = (registrations ?? []).filter(
        (registration) =>
            registration.briefingId && TRACKED_STATUSES.has(registration.status),
    );

    const pendingId = deliverMutation.isPending
        ? deliverMutation.variables?.registrationId
        : null;
    const deliverError =
        deliverMutation.isError && deliverMutation.variables
            ? {
                  registrationId: deliverMutation.variables.registrationId,
                  message: deliverMutation.error.message,
              }
            : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Safety briefing tracker
                </h2>

                <div className="mb-4 rounded-md border-l-[3px] border-primary bg-[#F2FBF9] px-3.5 py-2.5 text-[13px] text-ink">
                    <span className="font-semibold">Briefing process:</span> 5–10 min at the
                    meeting point, delivered by the front desk. A confirmed registration creates a
                    pending briefing; proof of delivery is the staff member plus the group leader's
                    signature. Site access is blocked until the briefing is marked as done.
                </div>

                <div className="mb-4 flex flex-wrap items-center gap-3">
                    <span className="text-xs font-semibold text-primary-dark">
                        {tracked.length} briefing{tracked.length === 1 ? "" : "s"} tracked
                    </span>
                    {/* The API has no reminder or export endpoint yet; the mockup's
                        buttons are kept visible but inert, with a screen-reader note. */}
                    <button
                        type="button"
                        disabled
                        aria-disabled="true"
                        className="cursor-not-allowed rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary opacity-60"
                    >
                        Remind pending
                        <span className="sr-only"> (not available yet)</span>
                    </button>
                    <button
                        type="button"
                        disabled
                        aria-disabled="true"
                        className="cursor-not-allowed rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary opacity-60"
                    >
                        Export log (PDF)
                        <span className="sr-only"> (not available yet)</span>
                    </button>
                </div>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading briefings…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Briefings could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        {/* Suggestions for the "delivered by" field of every row. */}
                        <datalist id="briefing-staff-names">
                            {(staff ?? []).map((member) => (
                                <option key={member.id} value={member.fullName} />
                            ))}
                        </datalist>

                        <div className="overflow-x-auto">
                            <table className="w-full border-collapse text-sm">
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
                                    {tracked.length === 0 && (
                                        <tr>
                                            <td
                                                colSpan={COLUMNS.length}
                                                className="px-3 py-8 text-center text-muted"
                                            >
                                                No briefing to track — confirm a registration first.
                                            </td>
                                        </tr>
                                    )}
                                    {tracked.map((registration) => (
                                        <BriefingRow
                                            key={registration.id}
                                            registration={registration}
                                            isDelivering={pendingId === registration.id}
                                            deliverError={
                                                deliverError?.registrationId === registration.id
                                                    ? deliverError.message
                                                    : null
                                            }
                                            onRecord={setRecording}
                                        />
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <p className="mt-6 text-[11px] text-muted">
                            Every briefing is timestamped and recorded with the delivering staff.
                            Site access is strictly blocked until the briefing is marked as done.
                        </p>
                    </>
                )}

                {recording && (
                    <Modal
                        title={`Record briefing — ${recording.visitorName ?? ""}`}
                        onClose={() => setRecording(null)}
                    >
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
                    </Modal>
                )}
            </section>
        </div>
    );
}