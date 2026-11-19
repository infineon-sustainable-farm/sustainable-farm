import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Table } from "../../../shared/ui";
import { useProspects } from "../hooks/useRegistrations";
import { useVisitorFormat } from "../utils/format";
import { useLabels } from "../utils/labels";
import { LoadError, LoadingBlock } from "./LoadState";
import StatusTag from "./StatusTag";

/*
 * The API flags every purchase, partnership or investment registration as a prospect, cancelled
 * and rejected ones included. The commercial follow-up only cares about the active ones, so those
 * are filtered out here.
 */
const ACTIVE_STATUSES = new Set(["PENDING", "CONFIRMED", "CHECKED_IN"]);

/**
 * Collapsible commercial follow-up section of the registration screen. The prospects query only
 * runs once the section is opened.
 */
export default function ProspectsSection() {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
    const [open, setOpen] = useState(false);
    const { data: prospects, isPending, isError, refetch, isFetching } = useProspects(open);

    const activeProspects = (prospects ?? []).filter((registration) =>
        ACTIVE_STATUSES.has(registration.status),
    );

    const columns = [
        {
            key: "visitorName",
            header: t("registration.prospects.columns.visitor"),
            cell: (registration) => registration.visitorName ?? format.none,
        },
        {
            key: "groupSize",
            header: t("registration.prospects.columns.group"),
            cell: (registration) => registration.groupSize ?? format.none,
        },
        {
            key: "visitPurpose",
            header: t("registration.prospects.columns.purpose"),
            cell: (registration) => labels.enum(registration.visitPurpose),
        },
        {
            key: "slot",
            header: t("registration.prospects.columns.slot"),
            className: "whitespace-nowrap",
            cell: (registration) =>
                t("registration.slotCell", {
                    date: format.dateShort(registration.slotDate),
                    time: format.time(registration.slotStart),
                }),
        },
        {
            key: "status",
            header: t("registration.prospects.columns.status"),
            cell: (registration) => <StatusTag value={registration.status} />,
        },
    ];

    return (
        <Card
            className="mt-6"
            titleAs="h3"
            title={t("registration.prospects.title")}
            subtitle={t("registration.prospects.subtitle")}
            actions={
                <Button variant="secondary" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
                    {open ? t("registration.prospects.hide") : t("registration.prospects.show")}
                </Button>
            }
        >
            {open && isPending && (
                <div className="mt-4">
                    <LoadingBlock label={t("registration.prospects.loading")} rows={3} />
                </div>
            )}

            {open && isError && (
                <LoadError
                    title={t("registration.prospects.loadError")}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                />
            )}

            {open && !isPending && !isError && (
                <>
                    <Table
                        className="mt-4"
                        caption={t("registration.prospects.title")}
                        columns={columns}
                        rows={activeProspects}
                        emptyMessage={t("registration.prospects.empty")}
                    />
                    <p className="mt-3 text-xs text-text-muted">{t("registration.prospects.footnote")}</p>
                </>
            )}
        </Card>
    );
}
