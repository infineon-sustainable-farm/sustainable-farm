import { useQuery } from "@tanstack/react-query";
import { fetchOperatorAssignments } from "../api/operatorAssignmentApi";

export function useOperatorAssignments(page) {
  return useQuery({
    queryKey: ["operator-assignments", page],
    queryFn: () => fetchOperatorAssignments(page),
  });
}
