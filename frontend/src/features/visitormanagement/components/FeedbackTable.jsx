import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Table } from "../../../shared/ui";
import { useVisitorFormat } from "../utils/format";
import RatingValue from "./RatingValue";

const MAX_TEAM_LENGTH = 60;

/**
 * The "Routed to" cell: the team is displayed once routed; until then a "route" button reveals a
 * small input. The API caps the team name at 60 characters.
 */
function RouteCell({ feedback, isRouting, routeError, onRoute }) {
    const { t } = useTranslation("visitormanagement");
    const [editing, setEditing] = useState(false);
    const [routedTo, setRoutedTo] = useState("");
    const [fieldError, setFieldError] = useState(null);

    if (feedback.routedTo) {
        return <span>{feedback.routedTo}</span>;
    }

    if (!editing) {
        return (
            <Button size="sm" variant="secondary" disabled={isRouting} onClick={() => setEditing(true)}>
                {t("feedback.route.open")}
            </Button>
        );
    }

    function handleSubmit(event) {
        event.preventDefault();
        const value = routedTo.trim();
        if (!value) {
            setFieldError(t("feedback.route.required"));
            return;
        }
        if (value.length > MAX_TEAM_LENGTH) {
            setFieldError(t("feedback.route.tooLong", { max: MAX_TEAM_LENGTH }));
            return;
        }
        setFieldError(null);
        onRoute(feedback.id, { routedTo: value });
        setEditing(false);
    }

    const error = fieldError ?? routeError;

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
                <Input
                    value={routedTo}
                    onChange={(event) => setRoutedTo(event.target.value)}
                    aria-label={t("feedback.route.teamLabel")}
                    placeholder={t("feedback.route.placeholder")}
                    aria-invalid={error ? true : undefined}
                    className="w-32"
                />
                <Button size="sm" type="submit" disabled={isRouting}>
                    {t("feedback.route.save")}
                </Button>
                <Button
                    size="sm"
                    variant="secondary"
                    disabled={isRouting}
                    onClick={() => {
                        setEditing(false);
                        setFieldError(null);
                    }}
                >
                    {t("feedback.route.cancel")}
                </Button>
            </div>
            {error && (
                <span role="alert" className="text-xs text-error-fg">
                    {error}
                </span>
            )}
        </form>
    );
}

/** The feedback responses, each one routable to the team that owns the follow-up. */
export default function FeedbackTable({ feedback, routingId, routeError, onRoute }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();

    const columns = [
        { key: "visitor", header: t("feedback.columns.visitor"), cell: (row) => row.visitorName ?? format.none },
        {
            key: "date",
            header: t("feedback.columns.date"),
            className: "whitespace-nowrap",
            cell: (row) => format.dateTime(row.submittedAt),
        },
        { key: "rating", header: t("feedback.columns.rating"), cell: (row) => <RatingValue value={row.rating} /> },
        {
            key: "comment",
            header: t("feedback.columns.comment"),
            className: "max-w-xs",
            cell: (row) => row.comment ?? format.none,
        },
        {
            key: "routedTo",
            header: t("feedback.columns.routedTo"),
            cell: (row) => (
                <RouteCell
                    feedback={row}
                    isRouting={routingId === row.id}
                    routeError={routeError?.id === row.id ? routeError.message : null}
                    onRoute={onRoute}
                />
            ),
        },
    ];

    return (
        <Table
            caption={t("feedback.responsesTitle")}
            columns={columns}
            rows={feedback}
            emptyMessage={t("feedback.responsesEmpty")}
        />
    );
}
