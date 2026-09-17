import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createOperatorAssignment,
  updateOperatorAssignment,
  deleteOperatorAssignment,
} from "../api/operatorAssignmentApi";

function useInvalidateOperatorAssignments() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["operator-assignments"] });
}

export function useCreateOperatorAssignment() {
  const invalidate = useInvalidateOperatorAssignments();
  return useMutation({
    mutationFn: createOperatorAssignment,
    onSuccess: invalidate,
  });
}

export function useUpdateOperatorAssignment() {
  const invalidate = useInvalidateOperatorAssignments();
  return useMutation({
    mutationFn: ({ id, data }) => updateOperatorAssignment(id, data),
    onSuccess: invalidate,
  });
}

export function useDeleteOperatorAssignment() {
  const invalidate = useInvalidateOperatorAssignments();
  return useMutation({
    mutationFn: deleteOperatorAssignment,
    onSuccess: invalidate,
  });
}
