import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, EmptyState, Table } from "../../../shared/ui";
import { useAddHealthIssue } from "../hooks/useAddHealthIssue";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatEnum, formatText, NO_VALUE } from "../utils/format";
import AddHealthIssueModal from "./AddHealthIssueModal";
import CellNote from "./CellNote";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";

/**
 * The catalogue, in the order the API sends it: pests, diseases, then "Other". The scientific name
 * sits in small muted text under the name, and the EPPO code under the source.
 */
function HealthIssuesTable({ issues }) {
    const { t } = useTranslation("plants");
    const columns = [
        {
            key: "name",
            header: t("health.issues.columns.name"),
            className: "align-top",
            cell: (issue) => (
                <>
                    {formatText(issue.name)}
                    {formatText(issue.scientificName) !== NO_VALUE && (
                        <CellNote className="italic">{formatText(issue.scientificName)}</CellNote>
                    )}
                </>
            ),
        },
        { key: "kind", header: t("health.issues.columns.kind"), className: "align-top", cell: (issue) => formatEnum("issueKind", issue.kind) },
        { key: "code", header: t("health.issues.columns.code"), className: "align-top", cell: (issue) => formatText(issue.code) },
        {
            key: "source",
            header: t("health.issues.columns.source"),
            className: "align-top",
            cell: (issue) => (
                <>
                    {formatText(issue.source)}
                    {formatText(issue.eppoCode) !== NO_VALUE && (
                        <CellNote>{t("health.issues.eppoNote", { code: formatText(issue.eppoCode) })}</CellNote>
                    )}
                </>
            ),
        },
    ];
    return <Table caption={t("health.issues.title")} columns={columns} rows={issues} />;
}

/**
 * Pests and diseases: the catalogue an inspection picks its problems from,
 * with the way to add one. It holds for every block, so the block filter does
 * not apply here.
 *
 * Outside the dev profile it starts empty, and until a pest or a disease is
 * added an inspection can report "Other" only. "Other" itself is listed once
 * an inspection has used it: the API adds it then.
 */
export default function HealthIssuesSection() {
    const { t } = useTranslation("plants");
    const issues = useHealthIssues();
    const addIssue = useAddHealthIssue();
    const [isAdding, setIsAdding] = useState(false);

    function openForm() {
        addIssue.reset();
        setIsAdding(true);
    }

    function closeForm() {
        addIssue.reset();
        setIsAdding(false);
    }

    function submitIssue(issue) {
        addIssue.mutate(issue, { onSuccess: closeForm });
    }

    return (
        <Card
            aria-labelledby="health-issues-title"
            title={<span id="health-issues-title">{t("health.issues.title")}</span>}
            subtitle={t("health.issues.subtitle")}
            actions={
                <Button variant="secondary" icon={Plus} onClick={openForm}>
                    {t("health.issues.add")}
                </Button>
            }
        >
            {issues.isPending && <HealthSectionLoading>{t("health.issues.loading")}</HealthSectionLoading>}

            {issues.isError && (
                <HealthSectionError
                    title={t("health.issues.loadError")}
                    detail={t("health.requestFailed")}
                    query={issues}
                />
            )}

            {!issues.isPending && !issues.isError && issues.data.length === 0 && (
                <EmptyState
                    className="py-8"
                    title={t("health.issues.emptyTitle")}
                    message={t("health.issues.emptyHint")}
                />
            )}

            {!issues.isPending && !issues.isError && issues.data.length > 0 && (
                <HealthIssuesTable issues={issues.data} />
            )}

            {isAdding && (
                <AddHealthIssueModal
                    isSubmitting={addIssue.isPending}
                    error={addIssue.error}
                    onSubmit={submitIssue}
                    onClose={closeForm}
                />
            )}
        </Card>
    );
}
