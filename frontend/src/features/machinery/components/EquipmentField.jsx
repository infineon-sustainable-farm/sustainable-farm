import { useTranslation } from "react-i18next";
import { Field, Select } from "../../../shared/ui";

/**
 * Equipment select of the machinery forms.
 *
 * @param {object} props
 * @param {string|null} props.emptyOptionLabel - label of the empty option, null for none. While the
 *   equipment list loads, the empty option reads "Loading equipment…".
 */
function EquipmentField({
  label,
  name = "equipmentId",
  value,
  onChange,
  error,
  equipments,
  isEquipmentLoading,
  emptyOptionLabel,
}) {
  const { t } = useTranslation("machinery");
  return (
    <Field label={label ?? t("fields.equipment")} error={error}>
      <Select name={name} value={value} onChange={onChange} disabled={isEquipmentLoading}>
        {emptyOptionLabel != null && (
          <option value="">{isEquipmentLoading ? t("common.loadingEquipment") : emptyOptionLabel}</option>
        )}
        {equipments.map((equipment) => (
          <option key={equipment.id} value={equipment.id}>
            {equipment.name}
          </option>
        ))}
      </Select>
    </Field>
  );
}
export default EquipmentField;
