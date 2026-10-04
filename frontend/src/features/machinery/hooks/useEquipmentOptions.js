import { useQuery } from "@tanstack/react-query";
import { fetchEquipments } from "../api/equipmentApi";

const EQUIPMENT_OPTIONS_SIZE = 100;

export function useEquipmentOptions() {
  return useQuery({
    queryKey: ["equipments", "options"],
    queryFn: () => fetchEquipments(0, EQUIPMENT_OPTIONS_SIZE),
  });
}
