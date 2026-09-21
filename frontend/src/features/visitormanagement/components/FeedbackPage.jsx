import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useFeedbackList,
    useFeedbackSummary,
    useRouteFeedback,
    useSendSurvey,
    useSubmitFeedback,
    useSurveys,
} from "../hooks/useFeedback";
import { useVisitors } from "../hooks/useRegistrations";
import { startOfWeek } from "../utils/format";
import FeedbackSummary from "./FeedbackSummary";
import FeedbackTable from "./FeedbackTable";
import SendSurveyForm from "./SendSurveyForm";
import SurveysTable from "./SurveysTable";
import TabletSurveyForm from "./TabletSurveyForm";

/**
 * Error/retry block shared by the three feedback sections, so each one can
 * fail independently without blanking the screen.
 */
function SectionError({ title, onRetry, isRetrying }) {
    return (
        <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-10 text-center">
            <TriangleAlert size={28} className="text-error" />
            <div>
                <p className="font-heading text-base font-bold text-ink">{title}</p>
                <p className="mt-1 text-sm text-muted">The request to the server did not succeed.</p>
            </div>
            <button
                type="button"
                onClick={onRetry}
                disabled={isRetrying}
                className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
            >
                {isRetrying ? "Retrying…" : "Retry"}
            </button>
        </div>
    );
}

function SectionLoading({ label }) {
    return (
        <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-10 text-sm text-muted">
            <Loader2 size={18} className="animate-spin" />
            {label}
        </div>
    );
}

export default function FeedbackPage() {
    const [sendFormOpen, setSendFormOpen] = useState(false);
    const [formKey, setFormKey] = useState(0);

    /*
     * The summary window is the current week, frozen once so the query key
     * stays stable across renders. The end is the end of today rather than the
     * current instant, so a response submitted later today still counts when
     * the summary is refetched after a submission.
     */
    const [summaryWindow] = useState(() => {
        const endOfDay = new Date();
        endOfDay.setHours(23, 59, 59, 999);
        return {
            from: startOfWeek(new Date()).toISOString(),
            to: endOfDay.toISOString(),
        };
    });

    useEffect(() => {
        document.title = "Satisfaction Survey — Visitor Management";
    }, []);

    const {
        data: feedback,
        isPending: feedbackPending,
        isError: feedbackError,
        refetch: refetchFeedback,
        isFetching: feedbackFetching,
    } = useFeedbackList();
    const {
        data: surveys,
        isPending: surveysPending,
        isError: surveysError,
        refetch: refetchSurveys,
        isFetching: surveysFetching,
    } = useSurveys();
    const {
        data: summary,
        isPending: summaryPending,
        isError: summaryError,
        refetch: refetchSummary,
        isFetching: summaryFetching,
    } = useFeedbackSummary(summaryWindow);
    const { data: visitors, isError: visitorsError } = useVisitors();

    const submitMutation = useSubmitFeedback();
    const sendMutation = useSendSurvey();
    const routeMutation = useRouteFeedback();

    const routingId = routeMutation.isPending ? routeMutation.variables?.id : null;
    const routeError = routeMutation.isError
        ? { id: routeMutation.variables?.id, message: routeMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Satisfaction survey
                </h2>

                <div className="mb-4 rounded-md border-l-[3px] border-primary bg-[#F2FBF9] px-3.5 py-2.5 text-[13px] text-ink">
                    <span className="font-semibold">Feedback flow (mixed):</span> visitors give a
                    quick rating on-site on the tablet at the wrap-up stop, and a full survey link is
                    sent by email or SMS after the visit. All responses are collected here.
                </div>

                <div className="mb-4 flex flex-wrap items-center gap-3">
                    <button
                        type="button"
                        onClick={() => {
                            sendMutation.reset();
                            setSendFormOpen((open) => !open);
                        }}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        Send survey (email/SMS)
                    </button>
                    {/* No report endpoint yet; the mockup's button stays inert. */}
                    <button
                        type="button"
                        disabled
                        aria-disabled="true"
                        className="cursor-not-allowed rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary opacity-60"
                    >
                        Compile report
                        <span className="sr-only"> (not available yet)</span>
                    </button>
                </div>

                {sendFormOpen && (
                    <SendSurveyForm
                        visitors={visitors ?? []}
                        isSubmitting={sendMutation.isPending}
                        submitError={sendMutation.error?.message}
                        serverFieldErrors={sendMutation.error?.data?.fieldErrors}
                        onSubmit={(values) =>
                            sendMutation.mutate(values, {
                                onSuccess: () => setSendFormOpen(false),
                            })
                        }
                        onCancel={() => {
                            sendMutation.reset();
                            setSendFormOpen(false);
                        }}
                    />
                )}

                {visitorsError && (
                    <p className="mb-4 text-xs text-muted">
                        Visitors could not be loaded — the survey and feedback forms need a visitor
                        and are unavailable until the list returns.
                    </p>
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    On-site tablet survey (wrap-up stop)
                </h3>
                <TabletSurveyForm
                    key={formKey}
                    visitors={visitors ?? []}
                    isSubmitting={submitMutation.isPending}
                    submitError={submitMutation.error?.message}
                    serverFieldErrors={submitMutation.error?.data?.fieldErrors}
                    onSubmit={(values) =>
                        submitMutation.mutate(values, {
                            onSuccess: () => setFormKey((current) => current + 1),
                        })
                    }
                />
                <p className="mt-3.5 text-[11px] text-muted">
                    Responses collected on the tablet are stored with the ON_SITE origin.
                </p>

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Sent surveys (after visit)
                </h3>
                {surveysPending && <SectionLoading label="Loading surveys…" />}
                {surveysError && (
                    <SectionError
                        title="Surveys could not be loaded"
                        onRetry={() => refetchSurveys()}
                        isRetrying={surveysFetching}
                    />
                )}
                {!surveysPending && !surveysError && <SurveysTable surveys={surveys ?? []} />}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">Summary</h3>
                {summaryPending && <SectionLoading label="Loading summary…" />}
                {summaryError && (
                    <SectionError
                        title="Summary could not be loaded"
                        onRetry={() => refetchSummary()}
                        isRetrying={summaryFetching}
                    />
                )}
                {!summaryPending && !summaryError && <FeedbackSummary summary={summary} />}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Recent responses
                </h3>
                {feedbackPending && <SectionLoading label="Loading responses…" />}
                {feedbackError && (
                    <SectionError
                        title="Responses could not be loaded"
                        onRetry={() => refetchFeedback()}
                        isRetrying={feedbackFetching}
                    />
                )}
                {!feedbackPending && !feedbackError && (
                    <>
                        <FeedbackTable
                            feedback={feedback ?? []}
                            routingId={routingId}
                            routeError={routeError}
                            onRoute={(id, data) => routeMutation.mutate({ id, data })}
                        />
                        <p className="mt-6 text-[11px] text-muted">
                            Quick ratings are collected on-site, full surveys go out by email or SMS
                            after the visit, and each response can be routed to the team that owns
                            the follow-up.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}