import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFuelLog, updateFuelLog, deleteFuelLog } from "../api/fuelLogApi";

function useInvalidateFuelLogs() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["fuel-logs"] });
}

export function useCreateFuelLog() {
  const invalidate = useInvalidateFuelLogs();
  return useMutation({
    mutationFn: createFuelLog,
    onSuccess: invalidate,
  });
}

export function useUpdateFuelLog() {
  const invalidate = useInvalidateFuelLogs();
  return useMutation({
    mutationFn: ({ id, data }) => updateFuelLog(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteFuelLog() {
  const invalidate = useInvalidateFuelLogs();
  return useMutation({
    mutationFn: deleteFuelLog,
    onSuccess: invalidate,
  });
}
