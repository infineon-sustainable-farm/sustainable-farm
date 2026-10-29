import { useQuery } from "@tanstack/react-query";
import { fetchLowStockSpareParts } from "../api/sparePartApi";

export function useLowStockSpareParts() {
  return useQuery({
    queryKey: ["spare-parts", "low-stock"],
    queryFn: fetchLowStockSpareParts,
  });
}
