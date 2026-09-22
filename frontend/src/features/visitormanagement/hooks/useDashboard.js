import { useQuery } from "@tanstack/react-query";
import { fetchDashboard } from "../api/visitormanagementApi";

/**
 * The Visitor Management dashboard data. Loading, error and retry are handled
 * by TanStack Query; the page reads its status flags instead of keeping its
 * own state. The backend always reports the current week, so when a new week
 * begins the next fetch already carries the fresh figures.
 */
export function useDashboard() {
    return useQuery({
        queryKey: ["visitormanagement", "dashboard"],
        queryFn: fetchDashboard,
    });
}