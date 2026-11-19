import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createUsageLog, updateUsageLog, deleteUsageLog } from "../api/usageLogApi";

function useInvalidateUsageLogs() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["usage-logs"] });
}

export function useCreateUsageLog() {
  const invalidate = useInvalidateUsageLogs();
  return useMutation({
    mutationFn: createUsageLog,
    onSuccess: invalidate,
  });
}

export function useUpdateUsageLog() {
  const invalidate = useInvalidateUsageLogs();
  return useMutation({
    mutationFn: ({ id, data }) => updateUsageLog(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteUsageLog() {
  const invalidate = useInvalidateUsageLogs();
  return useMutation({
    mutationFn: deleteUsageLog,
    onSuccess: invalidate,
  });
}
