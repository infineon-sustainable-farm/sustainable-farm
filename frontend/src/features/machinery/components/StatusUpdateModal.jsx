import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal, Select } from "../../../shared/ui";
import { STATUS } from "../constants";
import { useEnumLabels } from "../format";

function StatusUpdateModal({ equipment, onClose, onSubmit }) {
  const { t } = useTranslation("machinery");
  const enumLabels = useEnumLabels();
  const { t: tCommon } = useTranslation("common");
  const [status, setStatus] = useState(equipment?.status ?? STATUS.OPERATIONAL);

  if (!equipment) return null;

  return (
    <Modal
      open
      onClose={onClose}
      title={t("equipment.statusTitle", { name: equipment.name })}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>{tCommon("actions.cancel")}</Button>
          <Button onClick={() => onSubmit(equipment.id, status)}>{tCommon("actions.save")}</Button>
        </>
      }
    >
      <Select aria-label={t("equipment.status")} value={status} onChange={(e) => setStatus(e.target.value)}>
        {Object.values(STATUS).map((value) => (
          <option key={value} value={value}>{enumLabels.status(value)}</option>
        ))}
      </Select>
    </Modal>
  );
}
export default StatusUpdateModal;
