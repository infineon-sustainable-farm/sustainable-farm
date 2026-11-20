import { Check, ClipboardCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";
import { useBriefing } from "../hooks/useSafety";
import { useVisitorFormat } from "../utils/format";
import StatusTag from "./StatusTag";

/*
 * Cells of one registration's briefing in the tracker table. The API exposes no list endpoint,
 * so each row reads its briefing by registration; TanStack Query shares one request between the
 * cells of a row.
 */
function useRowBriefing(registration) {
    return useBriefing(registration.id, Boolean(registration.briefingId));
}

/** Delivery time, "…" while the briefing loads. */
export function BriefingTime({ registration }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const { data: briefing, isPending } = useRowBriefing(registration);
    return isPending ? t("safety.row.pending") : format.dateTime(briefing?.deliveredAt);
}

/** Who delivered the briefing. */
export function BriefingStaff({ registration }) {
    const format = useVisitorFormat();
    const { data: briefing } = useRowBriefing(registration);
    return briefing?.staffMember ?? format.none;
}

/** Whether the group leader signed. */
export function BriefingSignature({ registration }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const { data: briefing } = useRowBriefing(registration);
    if (!briefing?.signature) return format.none;
    return (
        <span className="inline-flex items-center gap-1">
            <Check size={16} aria-hidden="true" className="text-success-fg" />
            {t("safety.row.signed")}
        </span>
    );
}

/** Briefing status, or its loading and error state with a retry. */
export function BriefingStatus({ registration }) {
    const { t } = useTranslation("visitormanagement");
    const { data: briefing, isPending, isError, refetch, isFetching } = useRowBriefing(registration);
    if (isPending) return <span className="text-xs text-text-muted">{t("safety.row.loading")}</span>;
    if (isError) {
        return (
            <span className="inline-flex items-center gap-2 text-xs text-error-fg">
                {t("safety.row.unavailable")}
                <Button size="sm" variant="ghost" onClick={() => refetch()} loading={isFetching}>
                    {t("safety.row.retry")}
                </Button>
            </span>
        );
    }
    return <StatusTag value={briefing?.status} />;
}

/** "record" opens the delivery form while the briefing is pending; the delivery error shows under it. */
export function BriefingActions({ registration, isDelivering, deliverError, onRecord }) {
    const { t } = useTranslation("visitormanagement");
    const { data: briefing } = useRowBriefing(registration);
    return (
        <div className="flex flex-col items-start gap-1">
            {briefing?.status === "PENDING" && (
                <Button
                    size="sm"
                    variant="secondary"
                    icon={ClipboardCheck}
                    loading={isDelivering}
                    onClick={() => onRecord(registration)}
                >
                    {isDelivering ? t("safety.row.recording") : t("safety.row.record")}
                </Button>
            )}
            {deliverError && (
                <p role="alert" className="max-w-64 text-xs whitespace-normal text-error-fg">
                    {deliverError}
                </p>
            )}
        </div>
    );
}
