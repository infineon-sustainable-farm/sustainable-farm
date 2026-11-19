import { useQuery } from "@tanstack/react-query";
import { fetchMachineryEquipment } from "../api/plantsApi";

/**
 * The machines of the machinery register, which owns them, as the list a
 * treatment picks its equipment from: `data` is the array of machines.
 *
 * Loading, error and retry are handled by TanStack Query; the form passes its
 * status flags to OwnedResourceSelect.
 */
export function useMachineryEquipment() {
    return useQuery({
        queryKey: ["plants", "machinery-equipment"],
        queryFn: fetchMachineryEquipment,
        select: (page) => page?.content ?? [],
    });
}
