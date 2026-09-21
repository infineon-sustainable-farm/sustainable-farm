import { useState } from "react";
import StatusBadge from "./StatusBadge";
import { formatDateTime } from "../utils/format";
import { useBriefing } from "../hooks/useSafety";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";

/**
 * One registration's briefing row. The briefing is fetched per row (the API
 * exposes no list endpoint), the delivery form is inline and only shown for a
 * PENDING briefing; the API refuses to deliver anything else.
 */
export default function BriefingRow({
    registration,
    isDelivering,
    deliverError,
    onDeliver,
}) {
    const {
        data: briefing,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useBriefing(registration.id, Boolean(registration.briefingId));

    const [recording, setRecording] = useState(false);
    const [staffMember, setStaffMember] = useState("");
    const [signature, setSignature] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    function handleSubmit(event) {
        event.preventDefault();
        const errors = {};
        if (!staffMember.trim()) {
            errors.staffMember = "Who delivered the briefing is required.";
        } else if (staffMember.trim().length > 150) {
            errors.staffMember = "Staff member must be at most 150 characters.";
        }
        if (signature.trim().length > 500) {
            errors.signature = "Signature must be at most 500 characters.";
        }
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onDeliver(registration.id, {
            staffMember: staffMember.trim(),
            signature: signature.trim() || null,
        });
    }

    const status = briefing?.status;

    return (
        <>
            <tr className="border-b border-line align-top">
                <td className="px-3 py-2.5 font-medium text-ink">
                    {registration.visitorName ?? "—"}
                </td>
                <td className="px-3 py-2.5 text-ink">{registration.groupSize ?? "—"}</td>
                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                    {isPending ? "…" : formatDateTime(briefing?.deliveredAt)}
                </td>
                <td className="px-3 py-2.5 text-ink">{briefing?.staffMember ?? "—"}</td>
                <td className="px-3 py-2.5 text-ink">
                    {briefing?.signature ? "✓ signed" : "—"}
                </td>
                <td className="px-3 py-2.5">
                    {isPending && <span className="text-xs text-muted">Loading…</span>}
                    {isError && (
                        <span className="text-xs text-error">
                            Unavailable{" "}
                            <button
                                type="button"
                                onClick={() => refetch()}
                                disabled={isFetching}
                                className="underline disabled:opacity-60"
                            >
                                retry
                            </button>
                        </span>
                    )}
                    {!isPending && !isError && <StatusBadge value={status} />}
                </td>
                <td className="px-3 py-2.5">
                    {status === "PENDING" && !recording && (
                        <button
                            type="button"
                            disabled={isDelivering}
                            onClick={() => setRecording(true)}
                            className={TINY_BUTTON}
                        >
                            record
                        </button>
                    )}
                    {status === "PENDING" && recording && (
                        <span className="text-[11px] text-muted">Recording below…</span>
                    )}
                </td>
            </tr>

            {recording && (
                <tr className="border-b border-line bg-[#F2FBF9]">
                    <td colSpan={7} className="px-3 py-3">
                        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
                            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                                <div>
                                    <label
                                        htmlFor={`briefing-staff-${registration.id}`}
                                        className="mb-1 block text-xs tracking-wide text-primary uppercase"
                                    >
                                        Delivered by
                                    </label>
                                    <input
                                        id={`briefing-staff-${registration.id}`}
                                        type="text"
                                        list="briefing-staff-names"
                                        value={staffMember}
                                        onChange={(event) => setStaffMember(event.target.value)}
                                        placeholder="e.g. S. Traoré"
                                        className={INPUT_CLASS}
                                    />
                                    {fieldErrors.staffMember && (
                                        <p className="mt-1 text-xs text-error">
                                            {fieldErrors.staffMember}
                                        </p>
                                    )}
                                </div>
                                <div>
                                    <label
                                        htmlFor={`briefing-signature-${registration.id}`}
                                        className="mb-1 block text-xs tracking-wide text-primary uppercase"
                                    >
                                        Signature (group leader)
                                    </label>
                                    <input
                                        id={`briefing-signature-${registration.id}`}
                                        type="text"
                                        value={signature}
                                        onChange={(event) => setSignature(event.target.value)}
                                        placeholder="Optional"
                                        className={INPUT_CLASS}
                                    />
                                    {fieldErrors.signature && (
                                        <p className="mt-1 text-xs text-error">
                                            {fieldErrors.signature}
                                        </p>
                                    )}
                                </div>
                            </div>

                            {deliverError && (
                                <p className="rounded-md border border-error/30 bg-error/5 px-3 py-2 text-xs text-error">
                                    {deliverError}
                                </p>
                            )}

                            <div className="flex gap-2.5">
                                <button
                                    type="submit"
                                    disabled={isDelivering}
                                    className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {isDelivering ? "Recording…" : "Record briefing"}
                                </button>
                                <button
                                    type="button"
                                    disabled={isDelivering}
                                    onClick={() => {
                                        setRecording(false);
                                        setFieldErrors({});
                                    }}
                                    className="rounded-md border border-line bg-white px-4 py-2 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </td>
                </tr>
            )}
        </>
    );
}