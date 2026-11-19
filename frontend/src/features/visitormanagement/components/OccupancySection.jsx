import { useTranslation } from "react-i18next";
import { Card } from "../../../shared/ui";
import { useOccupancy } from "../hooks/useBooking";
import { LoadError, LoadingBlock } from "./LoadState";
import OccupancyBars from "./OccupancyBars";

/** Occupancy per activity on the booking screen, with its own loading and error states. */
export default function OccupancySection() {
    const { t } = useTranslation("visitormanagement");
    const { data: occupancy, isPending, isError, refetch, isFetching } = useOccupancy();

    return (
        <Card title={t("booking.occupancy.title")}>
            {isPending && <LoadingBlock label={t("booking.occupancy.loading")} rows={3} />}
            {isError && (
                <LoadError title={t("booking.occupancy.loadError")} onRetry={() => refetch()} retrying={isFetching} />
            )}
            {!isPending && !isError && (
                <>
                    <OccupancyBars occupancy={occupancy ?? []} />
                    <p className="mt-6 text-xs text-text-muted">{t("booking.occupancy.note")}</p>
                </>
            )}
        </Card>
    );
}
