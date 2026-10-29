import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createRepairLog, updateRepairLog, deleteRepairLog } from "../api/repairLogApi";

function useInvalidateRepairLogs() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["repair-logs"] });
}

export function useCreateRepairLog() {
  const invalidate = useInvalidateRepairLogs();
  return useMutation({
    mutationFn: createRepairLog,
    onSuccess: invalidate,
  });
}

export function useUpdateRepairLog() {
  const invalidate = useInvalidateRepairLogs();
  return useMutation({
    mutationFn: ({ id, data }) => updateRepairLog(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteRepairLog() {
  const invalidate = useInvalidateRepairLogs();
  return useMutation({
    mutationFn: deleteRepairLog,
    onSuccess: invalidate,
  });
}
