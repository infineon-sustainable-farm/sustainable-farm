import { useTranslation } from "react-i18next";
import { useVisitorFormat } from "../../../utils/format";
import { useLabels } from "../../../utils/labels";

/** The directory record found for the typed full name, reused as is by the registration. */
export default function FetchedVisitorCard({ visitor }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
    return (
        <>
            <div className="mt-2 rounded-control border border-border-subtle bg-primary-tint px-3 py-2 text-sm">
                <div className="flex items-center justify-between gap-2">
                    <span className="font-semibold text-text">{visitor.fullName}</span>
                    <span className="text-xs text-text-muted">{labels.enum(visitor.type)}</span>
                </div>
                <div className="mt-1.5 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-text-muted">
                    <span>
                        <strong className="font-semibold text-text">{visitor.groupSize}</strong>{" "}
                        {t("registration.fetched.people", { count: visitor.groupSize })}
                    </span>
                    <span>
                        {t("registration.fetched.language", {
                            value: visitor.language ? labels.language(visitor.language) : format.none,
                        })}
                    </span>
                    <span>{t("registration.fetched.email", { value: visitor.email ?? format.none })}</span>
                    <span>{t("registration.fetched.phone", { value: visitor.phone ?? format.none })}</span>
                    {visitor.specialNeeds && (
                        <span className="col-span-2">
                            {t("registration.fetched.needs", { value: visitor.specialNeeds })}
                        </span>
                    )}
                </div>
            </div>
            <p className="mt-1.5 text-xs text-text-muted">{t("registration.fetched.hint")}</p>
        </>
    );
}
