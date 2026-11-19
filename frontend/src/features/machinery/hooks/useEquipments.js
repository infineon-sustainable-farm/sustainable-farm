import { useQuery } from "@tanstack/react-query";
import { fetchEquipments } from "../api/equipmentApi";

export function useEquipments(page) {
  return useQuery({
    queryKey: ["equipments", page],
    queryFn: () => fetchEquipments(page),
  });
}