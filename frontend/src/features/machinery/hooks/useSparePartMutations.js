import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createSparePart, updateSparePart, deleteSparePart } from "../api/sparePartApi";

function useInvalidateSpareParts() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["spare-parts"] });
}

export function useCreateSparePart() {
  const invalidate = useInvalidateSpareParts();
  return useMutation({
    mutationFn: createSparePart,
    onSuccess: invalidate,
  });
}

export function useUpdateSparePart() {
  const invalidate = useInvalidateSpareParts();
  return useMutation({
    mutationFn: ({ id, data }) => updateSparePart(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteSparePart() {
  const invalidate = useInvalidateSpareParts();
  return useMutation({
    mutationFn: deleteSparePart,
    onSuccess: invalidate,
  });
}
