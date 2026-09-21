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

/** The feedback responses, one page of up to 100 rows, newest first. */
export function useFeedbackList() {
    const params = { page: 0, size: 100 };
    return useQuery({
        queryKey: [...FEEDBACK_KEY, params],
        queryFn: () => fetchFeedback(params),
        select: (page) => page.content,
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

/** The post-visit surveys, one page of up to 100 rows, newest first. */
export function useSurveys() {
    const params = { page: 0, size: 100 };
    return useQuery({
        queryKey: [...SURVEYS_KEY, params],
        queryFn: () => fetchSurveys(params),
        select: (page) => page.content,
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