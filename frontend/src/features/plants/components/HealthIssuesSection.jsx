import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, EmptyState, Table } from "../../../shared/ui";
import { useAddHealthIssue } from "../hooks/useAddHealthIssue";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatEnum, formatText } from "../utils/format";
import AddHealthIssueModal from "./AddHealthIssueModal";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";

/** The catalogue, in the order the API sends it: pests, diseases, then "Other". */
function HealthIssuesTable({ issues }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "name", header: t("health.issues.columns.name"), cell: (issue) => formatText(issue.name) },
        { key: "kind", header: t("health.issues.columns.kind"), cell: (issue) => formatEnum("issueKind", issue.kind) },
        { key: "code", header: t("health.issues.columns.code"), cell: (issue) => formatText(issue.code) },
        {
            key: "scientificName",
            header: t("health.issues.columns.scientificName"),
            className: "italic",
            cell: (issue) => formatText(issue.scientificName),
        },
        { key: "eppoCode", header: t("health.issues.columns.eppoCode"), cell: (issue) => formatText(issue.eppoCode) },
        { key: "source", header: t("health.issues.columns.source"), cell: (issue) => formatText(issue.source) },
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
