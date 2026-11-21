import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../../shared/ui";
import { useLabels } from "../../../utils/labels";
import { VISITOR_LANGUAGES } from "../visitorRules";
import { VISITOR_TYPE_CHOICES } from "./rules";

/** Directory fields of the visitor, edited when the form is opened from the registrations table. */
export default function VisitorDetailsFields({ values, onChange, fieldError }) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const set = (name) => (event) => onChange(name, event.target.value);

    return (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Field id="reg-group" label={t("registration.fields.groupSize")} error={fieldError("groupSize")}>
                <Input
                    type="number"
                    min="1"
                    value={values.groupSize}
                    onChange={set("groupSize")}
                    placeholder={t("registration.placeholders.groupSize")}
                />
            </Field>

            <Field id="reg-email" label={t("registration.fields.email")} error={fieldError("email")}>
                <Input
                    type="email"
                    value={values.email}
                    onChange={set("email")}
                    placeholder={t("visitors.placeholders.email")}
                />
            </Field>

            <Field id="reg-phone" label={t("registration.fields.phone")} error={fieldError("phone")}>
                <Input
                    type="tel"
                    value={values.phone}
                    onChange={set("phone")}
                    placeholder={t("visitors.placeholders.phone")}
                />
            </Field>

            <Field id="reg-language" label={t("registration.fields.language")}>
                <Select
                    value={values.language}
                    onChange={set("language")}
                    options={VISITOR_LANGUAGES.map((option) => ({ value: option, label: labels.language(option) }))}
                />
            </Field>

            <Field id="reg-type" label={t("registration.fields.type")}>
                <Select
                    value={values.visitorType}
                    onChange={set("visitorType")}
                    options={VISITOR_TYPE_CHOICES.map((choice) => ({ value: choice.value, label: t(choice.labelKey) }))}
                />
            </Field>

            <Field id="reg-needs" label={t("registration.fields.specialNeeds")} error={fieldError("specialNeeds")}>
                <Input
                    type="text"
                    value={values.specialNeeds}
                    onChange={set("specialNeeds")}
                    placeholder={t("visitors.placeholders.specialNeeds")}
                />
            </Field>
        </div>
    );
}
