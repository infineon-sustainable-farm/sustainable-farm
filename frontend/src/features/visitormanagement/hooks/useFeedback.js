import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    fetchFeedback,
    fetchFeedbackSummary,
    fetchSurveys,
    routeFeedback,
    sendSurvey,
    submitFeedback,
} from "../api/visitormanagementApi";

const FEEDBACK_KEY = ["visitormanagement", "feedback"];
const SURVEYS_KEY = ["visitormanagement", "surveys"];

/**
 * The feedback responses. Filters are exclusive on the backend: a visitor, or
 * a complete date range, otherwise the paginated list. The select normalises
 * the array and Page shapes to one list.
 */
export function useFeedbackList(filters = {}) {
    const { visitorId = "", from = "", to = "" } = filters;
    const query = visitorId
        ? { visitorId }
        : from && to
          ? { from, to }
          : { page: 0, size: 100 };
    return useQuery({
        queryKey: [...FEEDBACK_KEY, query],
        queryFn: () => fetchFeedback(query),
        select: (data) => (Array.isArray(data) ? data : data.content),
    });
}

/**
 * The feedback summary for a fixed window. The caller must keep the window
 * stable (it is part of the cache key), otherwise every render would look like
 * a new query.
 */
export function useFeedbackSummary({ from, to }) {
    return useQuery({
        queryKey: [...FEEDBACK_KEY, "summary", { from, to }],
        queryFn: () => fetchFeedbackSummary({ from, to }),
    });
}

/**
 * The post-visit surveys. A status filter switches the API to a plain array;
 * the select normalises both shapes to a list.
 */
export function useSurveys(filters = {}) {
    const { status = "" } = filters;
    const query = status ? { status } : { page: 0, size: 100 };
    return useQuery({
        queryKey: [...SURVEYS_KEY, query],
        queryFn: () => fetchSurveys(query),
        select: (data) => (Array.isArray(data) ? data : data.content),
    });
}

function useInvalidateFeedback() {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: FEEDBACK_KEY });
        queryClient.invalidateQueries({ queryKey: SURVEYS_KEY });
    };
}

/** Submits an on-site response; it moves the summary and the survey list. */
export function useSubmitFeedback() {
    const invalidate = useInvalidateFeedback();
    return useMutation({
        mutationFn: submitFeedback,
        onSettled: invalidate,
    });
}

/** Sends a survey; only the survey list changes. */
export function useSendSurvey() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: sendSurvey,
        onSettled: () =>
            queryClient.invalidateQueries({ queryKey: SURVEYS_KEY }),
    });
}

/** Routes a response; only the response list changes. */
export function useRouteFeedback() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, data }) => routeFeedback(id, data),
        onSettled: () =>
            queryClient.invalidateQueries({ queryKey: FEEDBACK_KEY }),
    });
}