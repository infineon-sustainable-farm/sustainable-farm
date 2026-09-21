import { useEffect } from "react";
import { X } from "lucide-react";
import VigorBadge from "./VigorBadge";
import Value from "./Value";
import {
    formatBlock,
    formatDensity,
    formatKilograms,
    formatMeters,
    formatNumber,
    formatSpacing,
    formatText,
    formatTimestamp,
} from "../utils/format";

function Field({ label, children }) {
    return (
        <div className="flex flex-col gap-1">
            <dt className="text-xs font-semibold tracking-wide text-gray-500 uppercase">
                {label}
            </dt>
            <dd className="text-sm text-gray-800">
                <Value>{children}</Value>
            </dd>
        </div>
    );
}

export default function VarietyDetailModal({ variety, onClose }) {
    useEffect(() => {
        function onKeyDown(event) {
            if (event.key === "Escape") onClose();
        }
        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);

    return (
        <div
            role="presentation"
            onClick={onClose}
            className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 p-4"
        >
            <div
                role="dialog"
                aria-modal="true"
                aria-labelledby="variety-detail-title"
                onClick={(event) => event.stopPropagation()}
                className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white shadow-xl"
            >
                <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                    <div>
                        <h3
                            id="variety-detail-title"
                            className="font-heading text-xl font-bold text-gray-900"
                        >
                            {formatText(variety.name)}
                        </h3>
                        <p className="mt-1 text-sm text-gray-500">
                            {formatBlock(variety.blockCode)}
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close"
                        className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
                    >
                        <X size={20} />
                    </button>
                </div>

                <dl className="grid grid-cols-1 gap-5 px-6 py-5 sm:grid-cols-2">
                    <Field label="Trees">{formatNumber(variety.treeCount)}</Field>
                    <Field label="Spacing">
                        {formatSpacing(
                            variety.rowSpacingM,
                            variety.treeSpacingM,
                        )}
                    </Field>
                    <Field label="Inter-row spacing">
                        {formatMeters(variety.rowSpacingM)}
                    </Field>
                    <Field label="Intra-row spacing">
                        {formatMeters(variety.treeSpacingM)}
                    </Field>
                    <Field label="Tree density">{formatDensity(variety.treeDensityPerHa)}</Field>
                    <Field label="Vigor">
                        <VigorBadge value={variety.vigor} />
                    </Field>
                    <Field label="Expected yield">
                        {formatKilograms(variety.expectedYieldKg)}
                    </Field>
                    <Field label="Actual yield">
                        {formatKilograms(variety.actualYieldKg)}
                    </Field>
                    <Field label="Plant origin">{formatText(variety.plantOrigin)}</Field>
                    <Field label="Farm ID">{formatNumber(variety.farmId)}</Field>
                    <Field label="Source">{formatText(variety.source)}</Field>
                    <Field label="Last updated">{formatTimestamp(variety.lastUpdated)}</Field>
                </dl>

                <p className="border-t border-gray-200 px-6 py-3 text-xs text-gray-500">
                    A dash means no value has been recorded for this field yet.
                </p>
            </div>
        </div>
    );
}
