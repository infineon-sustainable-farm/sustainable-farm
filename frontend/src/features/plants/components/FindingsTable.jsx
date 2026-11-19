import { CircleCheckBig, SprayCan } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatText } from "../utils/format";
import { findingPlace } from "../utils/healthIssues";
import FindingStatusBadge from "./FindingStatusBadge";
import Value from "./Value";

/** What a finding is and where, for the name of its buttons: "Anthracnose on Block A, tree 12". */
function useDescribeFinding() {
    const { t } = useTranslation("plants");
    return (finding) =>
        t("health.findings.describe", {
            problem: formatText(finding.otherLabel ?? finding.issueName),
            place: findingPlace(finding),
        });
}

/**
 * The findings, one row per problem seen, in the order the API sends them. A
 * finding not resolved yet can be treated or resolved; a resolved one can be
 * neither, as the API refuses both.
 *
 * The columns the API records ("Tree" is the tree label, "Status" the treatment
 * status), then the harvest and resolution dates. A finding has no source.
 */
export default function FindingsTable({ findings, onTreat, onResolve }) {
    const { t } = useTranslation("plants");
    const describeFinding = useDescribeFinding();

    const columns = [
        { key: "block", header: t("health.findings.columns.block"), cell: (finding) => formatBlock(finding.blockCode) },
        {
            key: "tree",
            header: t("health.findings.columns.tree"),
            cell: (finding) => <Value>{formatText(finding.treeLabel)}</Value>,
        },
        {
            // "Other" is named by the label typed with it, as in the API's alerts.
            key: "problem",
            header: t("health.findings.columns.problem"),
            className: "min-w-36 whitespace-normal",
            cell: (finding) => formatText(finding.otherLabel ?? finding.issueName),
        },
        {
            key: "observedOn",
            header: t("health.findings.columns.observedOn"),
            className: "whitespace-nowrap",
            cell: (finding) => formatDate(finding.inspectedOn),
        },
        {
            key: "status",
            header: t("health.findings.columns.status"),
            cell: (finding) => <FindingStatusBadge status={finding.status} />,
        },
        {
            key: "harvestAllowedFrom",
            header: t("health.findings.columns.harvestAllowedFrom"),
            className: "whitespace-nowrap",
            cell: (finding) => <Value>{formatDate(finding.harvestAllowedFrom)}</Value>,
        },
        {
            key: "resolvedOn",
            header: t("health.findings.columns.resolvedOn"),
            className: "whitespace-nowrap",
            cell: (finding) => <Value>{formatDate(finding.resolvedOn)}</Value>,
        },
    ];

    return (
        <Table
            caption={t("health.findings.title")}
            columns={columns}
            rows={findings}
            rowActions={(finding) =>
                finding.resolvedOn === null && (
                    <>
                        <Button
                            size="sm"
                            variant="secondary"
                            icon={SprayCan}
                            onClick={() => onTreat(finding)}
                            aria-label={t("health.findings.treatLabel", { finding: describeFinding(finding) })}
                        >
                            {t("health.findings.treat")}
                        </Button>
                        <Button
                            size="sm"
                            variant="secondary"
                            icon={CircleCheckBig}
                            onClick={() => onResolve(finding)}
                            aria-label={t("health.findings.resolveLabel", { finding: describeFinding(finding) })}
                        >
                            {t("health.findings.resolve")}
                        </Button>
                    </>
                )
            }
        />
    );
}
