import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useFeedbackList,
    useRouteFeedback,
    useSendSurvey,
    useSubmitFeedback,
    useSurveys,
} from "../../hooks/useFeedback";
import { useVisitors } from "../../hooks/useVisitors";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";
import FeedbackTable from "../FeedbackTable";
import Modal from "../Modal";
import SendSurveyForm from "../Forms/SendSurveyForm";
import SurveysTable from "../SurveysTable";
import TabletSurveyForm from "../Forms/TabletSurveyForm";

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
    const [tabletFormOpen, setTabletFormOpen] = useState(false);
    const [formKey, setFormKey] = useState(0);
    const [feedbackFilters, setFeedbackFilters] = useState({ visitorId: "", from: "", to: "" });
    const [surveyStatus, setSurveyStatus] = useState("");

    /*
     * The API applies visitorId first and the date range only when both bounds
     * are given, so the controls clear each other and the range stays idle
     * until it is complete.
     */
    function updateVisitorFilter(value) {
        setFeedbackFilters({ visitorId: value, from: "", to: "" });
    }

    function updateDateFilter(key, value) {
        setFeedbackFilters((current) => ({
            visitorId: "",
            from: key === "from" ? value : current.from,
            to: key === "to" ? value : current.to,
        }));
    }

    const hasFeedbackFilter = Boolean(
        feedbackFilters.visitorId || feedbackFilters.from || feedbackFilters.to,
    );
    const incompleteRange =
        !feedbackFilters.visitorId &&
        ((feedbackFilters.from && !feedbackFilters.to) ||
            (!feedbackFilters.from && feedbackFilters.to));

    useEffect(() => {
        document.title = "Satisfaction Survey — Visitor Management";
    }, []);

    const {
        data: feedback,
        isPending: feedbackPending,
        isError: feedbackError,
        refetch: refetchFeedback,
        isFetching: feedbackFetching,
    } = useFeedbackList(feedbackFilters);
    const {
        data: surveys,
        isPending: surveysPending,
        isError: surveysError,
        refetch: refetchSurveys,
        isFetching: surveysFetching,
    } = useSurveys({ status: surveyStatus });
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
            <section className="rounded-lg border border-line bg-white p-7 animate-fade-up">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Satisfaction survey
                </h2>

                <div className="mb-4 rounded-md border-l-[3px] border-primary bg-[#F2FBF9] px-3.5 py-2.5 text-[13px] text-ink">
                    <span className="font-semibold">Feedback flow:</span> visitors give a quick
                    rating on-site on the tablet at the wrap-up stop, and the survey link is sent
                    by email after the visit. All responses are collected here.
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
                        Send survey (email)
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
                    <Modal title="Send survey" onClose={() => setSendFormOpen(false)}>
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
                    </Modal>
                )}

                {visitorsError && (
                    <p className="mb-4 text-xs text-muted">
                        Visitors could not be loaded — sending a survey needs a visitor and is
                        unavailable until the list returns.
                    </p>
                )}

                <div className="mb-3 mt-6.5 flex flex-wrap items-center gap-4">
                    <h3 className="font-heading text-[17px] font-bold text-ink">
                        On-site tablet survey (wrap-up stop)
                    </h3>
                    <button
                        type="button"
                        onClick={() => {
                            submitMutation.reset();
                            setTabletFormOpen(true);
                            setFormKey((current) => current + 1);
                        }}
                        className="ml-auto rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New on-site response
                    </button>
                </div>

                <p className="text-[11px] text-muted">
                    Responses collected on the tablet are stored with the ON_SITE origin.
                </p>

                {tabletFormOpen && (
                    <Modal
                        title="On-site tablet survey"
                        onClose={() => setTabletFormOpen(false)}
                        widthClass="max-w-3xl"
                    >
                        <TabletSurveyForm
                            key={formKey}
                            isSubmitting={submitMutation.isPending}
                            submitError={submitMutation.error?.message}
                            serverFieldErrors={submitMutation.error?.data?.fieldErrors}
                            onSubmit={(values) =>
                                submitMutation.mutate(values, {
                                    onSuccess: () => setTabletFormOpen(false),
                                })
                            }
                            onCancel={() => setTabletFormOpen(false)}
                        />
                    </Modal>
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Sent surveys (after visit)
                </h3>

                <div className="mb-4 flex flex-wrap items-end gap-3.5">
                    <div>
                        <label
                            htmlFor="sv-filter-status"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Status
                        </label>
                        <select
                            id="sv-filter-status"
                            value={surveyStatus}
                            onChange={(event) => setSurveyStatus(event.target.value)}
                            className="min-w-[150px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All statuses</option>
                            {["SENT", "RECEIVED"].map((status) => (
                                <option key={status} value={status}>
                                    {formatEnumLabel(status)}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {surveysPending && <SectionLoading label="Loading surveys…" />}
                {surveysError && (
                    <SectionError
                        title="Surveys could not be loaded"
                        onRetry={() => refetchSurveys()}
                        isRetrying={surveysFetching}
                    />
                )}
                {!surveysPending && !surveysError && <SurveysTable surveys={surveys ?? []} />}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Recent responses
                </h3>

                <div className="mb-4 flex flex-wrap items-end gap-3.5">
                    <div>
                        <label
                            htmlFor="fb-filter-visitor"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Visitor
                        </label>
                        <select
                            id="fb-filter-visitor"
                            value={feedbackFilters.visitorId}
                            onChange={(event) => updateVisitorFilter(event.target.value)}
                            className="min-w-[190px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All visitors</option>
                            {(visitors ?? []).map((visitor) => (
                                <option key={visitor.id} value={visitor.id}>
                                    {visitor.fullName}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="fb-filter-from"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            From
                        </label>
                        <input
                            id="fb-filter-from"
                            type="date"
                            value={feedbackFilters.from}
                            onChange={(event) => updateDateFilter("from", event.target.value)}
                            className="rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="fb-filter-to"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            To
                        </label>
                        <input
                            id="fb-filter-to"
                            type="date"
                            value={feedbackFilters.to}
                            onChange={(event) => updateDateFilter("to", event.target.value)}
                            className="rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        />
                    </div>

                    {hasFeedbackFilter && (
                        <button
                            type="button"
                            onClick={() =>
                                setFeedbackFilters({ visitorId: "", from: "", to: "" })
                            }
                            className="rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary hover:bg-[#F2FBF9]"
                        >
                            Clear filters
                        </button>
                    )}

                    {incompleteRange && (
                        <p className="text-xs text-muted">
                            Set both dates to filter on a period.
                        </p>
                    )}
                </div>

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
                            Quick ratings are collected on-site, the survey link goes out by email
                            after the visit, and each response can be routed to the team that owns
                            the follow-up.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}