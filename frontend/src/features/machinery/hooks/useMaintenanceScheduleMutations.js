import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createMaintenanceSchedule,
  updateMaintenanceSchedule,
  deleteMaintenanceSchedule,
} from "../api/maintenanceScheduleApi";

function useInvalidateMaintenanceSchedules() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["maintenance-schedules"] });
}

export function useCreateMaintenanceSchedule() {
  const invalidate = useInvalidateMaintenanceSchedules();
  return useMutation({
    mutationFn: createMaintenanceSchedule,
    onSuccess: invalidate,
  });
}

export function useUpdateMaintenanceSchedule() {
  const invalidate = useInvalidateMaintenanceSchedules();
  return useMutation({
    mutationFn: ({ id, data }) => updateMaintenanceSchedule(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteMaintenanceSchedule() {
  const invalidate = useInvalidateMaintenanceSchedules();
  return useMutation({
    mutationFn: deleteMaintenanceSchedule,
    onSuccess: invalidate,
  });
}
