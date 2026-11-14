import { cn } from "../utils/cn";
import { emailDeliveryLabel, emailKindLabel, emailSubmissionLabel, formatInstant, isSubmissionFailed } from "../utils/userStatus";

/** "Invitation email · Submitted to provider · Delivery not confirmed". Never says "Delivered". */
export default function EmailStateLine({ delivery, compact = false }) {
    if (!delivery) return <span className="font-sans text-[12px] text-slate-600">No email</span>;
    const failed = isSubmissionFailed(delivery.submission);
    return (<div className="font-sans text-[12px] leading-snug">
      {!compact && <p className="font-semibold text-navy-900">{emailKindLabel(delivery.kind)}</p>}
      <p className={cn(failed ? "font-semibold text-rose-700" : "text-slate-600")}>
        {emailSubmissionLabel(delivery.submission)}
        {delivery.attempts > 1 && ` · ${delivery.attempts} attempts`}
      </p>
      <p className={delivery.delivery === "confirmed_by_admin" ? "text-emerald-700" : "text-slate-600"}>{emailDeliveryLabel(delivery.delivery)}</p>
      {!compact && delivery.submittedAt && <p className="font-mono text-[11px] text-slate-600">Submitted {formatInstant(delivery.submittedAt)}</p>}
      {!compact && delivery.lastError && <p className="text-[11.5px] text-rose-700">{delivery.lastError}</p>}
      {!compact && delivery.delivery === "confirmed_by_admin" && (<p className="text-[11.5px] text-slate-600">
          Manual attestation{delivery.deliveryConfirmedByEmail ? ` by ${delivery.deliveryConfirmedByEmail}` : ""}
          {delivery.deliveryConfirmedAt ? ` on ${formatInstant(delivery.deliveryConfirmedAt)}` : ""}
          {delivery.deliveryEvidence ? ` · Evidence: ${delivery.deliveryEvidence}` : ""}
          {" · not a provider delivery report"}
        </p>)}
    </div>);
}
