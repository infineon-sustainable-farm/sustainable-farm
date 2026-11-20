import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { useVisitorFormat } from "../utils/format";
import { useLabels } from "../utils/labels";
import RatingValue from "./RatingValue";
import StatusTag from "./StatusTag";

/** The surveys sent after a visit. */
export default function SurveysTable({ surveys }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();

    const columns = [
        { key: "visitor", header: t("feedback.columns.visitor"), cell: (row) => row.visitorName ?? format.none },
        { key: "channel", header: t("feedback.columns.channel"), cell: (row) => labels.enum(row.channel) },
        {
            key: "sentAt",
            header: t("feedback.columns.sentAt"),
            className: "whitespace-nowrap",
            cell: (row) => format.dateTime(row.sentAt),
        },
        { key: "status", header: t("feedback.columns.status"), cell: (row) => <StatusTag value={row.status} /> },
        {
            key: "rating",
            header: t("feedback.columns.rating"),
            cell: (row) => (row.rating ? <RatingValue value={row.rating} /> : format.none),
        },
    ];

    return (
        <Table
            caption={t("feedback.surveysTitle")}
            columns={columns}
            rows={surveys}
            emptyMessage={t("feedback.surveysEmpty")}
        />
    );
}
