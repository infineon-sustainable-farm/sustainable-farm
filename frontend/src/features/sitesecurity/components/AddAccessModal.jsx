import { useState } from "react";
import { useTranslation } from "react-i18next";
import { KeyRound } from "lucide-react";
import PersonSelect from "../../../app/PersonSelect";
import { Button, Field, Input, Modal, Select } from "../../../shared/ui";
import { LEVEL_ORDER } from "../api/sitesecurityTypes";
import { InlineAlert, SensitivityMeter } from "./ui";
import {
  CREDENTIAL_TYPES,
  allowedLevels,
  buildCredentialPayload,
  defaultValidUntil,
  isCredentialFormComplete,
  levelForType,
  toIsoDate,
} from "../utils/credentialForm";
import { translateEnum } from "../utils/status";

const EMPTY_FORM = () => ({
  personId: "",
  role: "",
  type: "Staff",
  level: "controlled",
  validUntil: defaultValidUntil(new Date()),
});

/*
 * Grants a site access to a person of the shared people directory (GET /api/staff/persons).
 * People are created only by the visitor management Staff screen; this form never asks for a
 * name or an email.
 */
export default function AddAccessModal({ open, onClose, onAdd }) {
  const { t } = useTranslation("sitesecurity");
  const { t: tCommon } = useTranslation("common");
  const [form, setForm] = useState(EMPTY_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const permittedLevels = allowedLevels(form.type);

  const update = (changes) => setForm((current) => ({ ...current, ...changes }));

  const close = () => {
    setForm(EMPTY_FORM());
    setError(null);
    setFieldErrors({});
    onClose();
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!isCredentialFormComplete(form) || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      await onAdd(buildCredentialPayload(form));
      close();
    } catch (err) {
      setError(err.message || t("access.form.saveError"));
      setFieldErrors(err.data?.fieldErrors ?? {});
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("access.form.title")}
      description={t("access.form.description")}
      size="md"
      footer={
        <>
          <Button variant="secondary" onClick={close}>
            {tCommon("actions.cancel")}
          </Button>
          <Button
            type="submit"
            form="sitesecurity-credential-form"
            icon={KeyRound}
            loading={isSubmitting}
            disabled={!isCredentialFormComplete(form)}
          >
            {t("access.form.submit")}
          </Button>
        </>
      }
    >
      <form id="sitesecurity-credential-form" onSubmit={submit} className="space-y-4" noValidate>
        {error && <InlineAlert>{error}</InlineAlert>}

        <PersonSelect
          label={t("access.form.person")}
          hint={t("access.form.personHint")}
          value={form.personId}
          onChange={(id) => update({ personId: id })}
          error={fieldErrors.personId}
          required
        />

        <Field label={t("access.form.role")} hint={t("access.form.roleHint")} error={fieldErrors.role} required>
          <Input maxLength={100} value={form.role} onChange={(e) => update({ role: e.target.value })} />
        </Field>

        <Field label={t("access.form.type")} error={fieldErrors.type} required>
          <Select
            value={form.type}
            onChange={(e) => update({ type: e.target.value, level: levelForType(e.target.value, form.level) })}
            options={CREDENTIAL_TYPES.map((type) => ({ value: type, label: translateEnum(t, "type", type) }))}
          />
        </Field>

        <Field label={t("access.form.level")} hint={t(`access.form.levelHint.${form.level}`)} error={fieldErrors.level} required>
          <Select
            value={form.level}
            onChange={(e) => update({ level: e.target.value })}
            options={LEVEL_ORDER.map((level) => ({
              value: level,
              label: t(`levels.${level}`),
              disabled: !permittedLevels.includes(level),
            }))}
          />
        </Field>
        <SensitivityMeter level={form.level} />

        <Field label={t("access.form.validUntil")} error={fieldErrors.validUntil} required>
          <Input
            type="date"
            value={form.validUntil}
            min={toIsoDate(new Date())}
            onChange={(e) => update({ validUntil: e.target.value })}
          />
        </Field>
      </form>
    </Modal>
  );
}
