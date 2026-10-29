import { useQuery } from "@tanstack/react-query";
import { fetchSpareParts } from "../api/sparePartApi";

export function useSpareParts(page, size = 10) {
  return useQuery({
    queryKey: ["spare-parts", page, size],
    queryFn: () => fetchSpareParts(page, size),
  });
}
