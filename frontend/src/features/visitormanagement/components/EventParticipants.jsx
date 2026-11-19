import { UserPlus, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useEventRegistrations } from "../hooks/useEvents";
import { Button, Card, IconButton, Table } from "../../../shared/ui";
import { useLabels } from "../utils/labels";
import { useVisitorFormat } from "../utils/format";
import { LoadError } from "./LoadState";
import StatusTag from "./StatusTag";

/**
 * Participants panel of one event, opened from a card. It is read-only: the attendee
 * registration form lives on the Registration screen (target: Event), which is the single entry
 * point for registering visitors.
 */
export default function EventParticipants({ event, onClose }) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const format = useVisitorFormat();
    const navigate = useNavigate();
    const { data: registrations, isPending, isError, refetch, isFetching } = useEventRegistrations(
        event.id,
    );

    function goToRegistration() {
        navigate(`/visitormanagement/registration?eventId=${event.id}`);
    }

    const columns = [
        {
            key: "visitorName",
            header: t("events.participants.columns.visitor"),
            cell: (registration) => registration.visitorName ?? format.none,
        },
        {
            key: "groupSize",
            header: t("events.participants.columns.group"),
            cell: (registration) => registration.groupSize ?? format.none,
        },
        {
            key: "visitPurpose",
            header: t("events.participants.columns.purpose"),
            cell: (registration) => labels.enum(registration.visitPurpose),
        },
        {
            key: "status",
            header: t("events.participants.columns.status"),
            cell: (registration) => <StatusTag value={registration.status} />,
        },
    ];

    const summary = [
        event.booked > 0
            ? t("events.registered", { value: format.number(event.booked) })
            : t("events.noRegistration"),
        t("events.participants.max", { value: format.number(event.maxCapacity) }),
    ].join(" · ");

    return (
        <Card
            title={t("events.participants.title", { title: event.title })}
            subtitle={summary}
            actions={
                <>
                    <Button
                        size="sm"
                        icon={UserPlus}
                        onClick={goToRegistration}
                        title={t("events.participants.registerHint")}
                    >
                        {t("events.participants.register")}
                    </Button>
                    <IconButton icon={X} tone="neutral" label={t("events.participants.close")} onClick={onClose} />
                </>
            }
        >
            {isError ? (
                <LoadError
                    title={t("events.participants.loadError")}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                />
            ) : (
                <Table
                    caption={t("events.participants.title", { title: event.title })}
                    columns={columns}
                    rows={registrations ?? []}
                    loading={isPending}
                    emptyMessage={t("events.participants.empty")}
                />
            )}

            <p className="mt-4 rounded-control bg-surface-muted px-3 py-2 text-xs text-text-muted">
                {t("events.participants.note")}
            </p>
        </Card>
    );
}
