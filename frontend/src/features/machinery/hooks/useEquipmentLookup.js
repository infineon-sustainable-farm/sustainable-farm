import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useEquipmentOptions } from "./useEquipmentOptions";

/**
 * Equipment list of the form selects and the name shown for an equipment id in the tables:
 * "—" without equipment, "#12" for an id that is not in the list.
 */
export function useEquipmentLookup() {
  const { t } = useTranslation("machinery");
  const equipmentOptions = useEquipmentOptions();

  const equipments = useMemo(() => equipmentOptions.data?.content ?? [], [equipmentOptions.data]);
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  const getEquipmentName = useCallback(
    (equipmentId) => {
      if (equipmentId == null) return t("common.none");
      return equipmentNames.get(equipmentId) ?? t("common.unknownEquipment", { id: equipmentId });
    },
    [equipmentNames, t]
  );

  return { equipments, isEquipmentLoading: equipmentOptions.isLoading, getEquipmentName };
}
