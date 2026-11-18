import { useTranslation } from "react-i18next";
import { Modal } from "../../../shared/ui";
import VigorBadge from "./VigorBadge";
import Value from "./Value";
import {
    formatBlock,
    formatDensity,
    formatMeters,
    formatNumber,
    formatSpacing,
    formatText,
    formatTimestamp,
} from "../utils/format";

function DetailField({ label, children }) {
    return (
        <div className="flex flex-col gap-1">
            <dt className="text-sm text-text-muted">{label}</dt>
            <dd className="text-sm text-text">
                <Value>{children}</Value>
            </dd>
        </div>
    );
}

export default function VarietyDetailModal({ variety, onClose }) {
    const { t } = useTranslation("plants");
    return (
        <Modal open onClose={onClose} size="xl" title={formatText(variety.name)} description={formatBlock(variety.blockCode)}>
            <dl className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <DetailField label={t("varieties.detail.treesPlanted")}>{formatNumber(variety.treeCount)}</DetailField>
                <DetailField label={t("varieties.detail.treesNow")}>{formatNumber(variety.currentTreeCount)}</DetailField>
                <DetailField label={t("varieties.detail.spacing")}>
                    {formatSpacing(variety.rowSpacingM, variety.treeSpacingM)}
                </DetailField>
                <DetailField label={t("varieties.detail.interRowSpacing")}>{formatMeters(variety.rowSpacingM)}</DetailField>
                <DetailField label={t("varieties.detail.intraRowSpacing")}>{formatMeters(variety.treeSpacingM)}</DetailField>
                <DetailField label={t("varieties.detail.treeDensity")}>{formatDensity(variety.treeDensityPerHa)}</DetailField>
                <DetailField label={t("varieties.detail.vigor")}>
                    <VigorBadge value={variety.vigor} />
                </DetailField>
                <DetailField label={t("varieties.detail.plantOrigin")}>{formatText(variety.plantOrigin)}</DetailField>
                <DetailField label={t("varieties.detail.farmId")}>{formatNumber(variety.farmId)}</DetailField>
                <DetailField label={t("varieties.detail.source")}>{formatText(variety.source)}</DetailField>
                <DetailField label={t("varieties.detail.lastUpdated")}>{formatTimestamp(variety.lastUpdated)}</DetailField>
            </dl>

            <p className="mt-5 text-xs text-text-muted">{t("varieties.detail.dashNote")}</p>
        </Modal>
    );
}
