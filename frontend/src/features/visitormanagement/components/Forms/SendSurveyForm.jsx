import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

const MAX_LINK_LENGTH = 500;

/*
 * Surveys are sent by email only. The backend resolves the recipient from the visitor, so the
 * channel is fixed and the email shown in the form is read-only information pulled from the
 * selected visitor. The message field holds the survey link rather than a prose template.
 */
export default function SendSurveyForm({
    visitors,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const [visitorId, setVisitorId] = useState("");
    const [surveyLink, setSurveyLink] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    const selectedVisitor = visitors.find((visitor) => visitor.id === Number(visitorId));
    const recipientEmail = selectedVisitor?.email ?? "";

    function validate() {
        const errors = {};
        if (!visitorId) errors.visitorId = t("feedback.send.visitorRequired");
        if (!surveyLink.trim()) errors.surveyLink = t("feedback.send.linkRequired");
        else if (surveyLink.trim().length > MAX_LINK_LENGTH) {
            errors.surveyLink = t("feedback.send.linkTooLong", { max: MAX_LINK_LENGTH });
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            visitorId: Number(visitorId),
            channel: "EMAIL",
            messageTemplate: surveyLink.trim(),
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field label={t("feedback.send.visitor")} error={fieldError("visitorId")}>
                    <Select value={visitorId} onChange={(event) => setVisitorId(event.target.value)}>
                        <option value="">{t("feedback.send.selectVisitor")}</option>
                        {visitors.map((visitor) => (
                            <option key={visitor.id} value={visitor.id}>
                                {visitor.fullName}
                            </option>
                        ))}
                    </Select>
                </Field>
                <Field
                    label={t("feedback.send.email")}
                    error={selectedVisitor && !recipientEmail ? t("feedback.send.noEmail") : undefined}
                >
                    <Input
                        type="email"
                        value={recipientEmail}
                        readOnly
                        placeholder={t("feedback.send.noEmailPlaceholder")}
                        className="bg-surface-muted"
                    />
                </Field>
            </div>

            <Field label={t("feedback.send.link")} error={fieldError("surveyLink")}>
                <Input
                    type="url"
                    value={surveyLink}
                    onChange={(event) => setSurveyLink(event.target.value)}
                    placeholder={t("feedback.send.linkPlaceholder")}
                />
            </Field>

            {submitError && <FormAlert>{submitError}</FormAlert>}

            <FormActions
                submitLabel={t("feedback.send.submit")}
                onCancel={onCancel}
                isSubmitting={isSubmitting}
            />
        </form>
    );
}
