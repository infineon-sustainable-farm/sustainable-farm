import { useEffect, useState } from "react";
import { Plus, Send } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, Modal, PageHeader, Select } from "../../../../shared/ui";
import {
    useFeedbackList,
    useRouteFeedback,
    useSendSurvey,
    useSubmitFeedback,
    useSurveys,
} from "../../hooks/useFeedback";
import { useVisitors } from "../../hooks/useVisitors";
import { useLabels } from "../../utils/labels";
import FeedbackTable from "../FeedbackTable";
import SendSurveyForm from "../Forms/SendSurveyForm";
import SurveysTable from "../SurveysTable";
import TabletSurveyForm from "../Forms/TabletSurveyForm";
import { LoadError, LoadingBlock } from "../LoadState";

const SURVEY_STATUSES = ["SENT", "RECEIVED"];
const NO_FILTER = { visitorId: "", from: "", to: "" };

export default function FeedbackPage() {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const [sendFormOpen, setSendFormOpen] = useState(false);
    const [tabletFormOpen, setTabletFormOpen] = useState(false);
    const [formKey, setFormKey] = useState(0);
    const [feedbackFilters, setFeedbackFilters] = useState(NO_FILTER);
    const [surveyStatus, setSurveyStatus] = useState("");

    /*
     * The API applies visitorId first and the date range only when both bounds are given, so the
     * controls clear each other and the range stays idle until it is complete.
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
        document.title = t("feedback.documentTitle");
    }, [t]);

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

    function closeSendForm() {
        sendMutation.reset();
        setSendFormOpen(false);
    }

    return (
        <div className="flex flex-col gap-6 p-6">
            <PageHeader
                title={t("feedback.title")}
                actions={
                    <Button
                        icon={Send}
                        onClick={() => {
                            sendMutation.reset();
                            setSendFormOpen(true);
                        }}
                    >
                        {t("feedback.sendSurvey")}
                    </Button>
                }
            />

            <p className="rounded-tile bg-primary-tint px-4 py-3 text-sm text-text">
                <span className="font-semibold">{t("feedback.flowLabel")}</span> {t("feedback.flowText")}
            </p>

            {visitorsError && <p className="text-xs text-text-muted">{t("feedback.visitorsError")}</p>}

            <Modal open={sendFormOpen} onClose={closeSendForm} title={t("feedback.send.title")} size="xl">
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
                    onCancel={closeSendForm}
                />
            </Modal>

            <Card
                title={t("feedback.tabletTitle")}
                subtitle={t("feedback.tabletNote")}
                actions={
                    <Button
                        icon={Plus}
                        onClick={() => {
                            submitMutation.reset();
                            setTabletFormOpen(true);
                            setFormKey((current) => current + 1);
                        }}
                    >
                        {t("feedback.newResponse")}
                    </Button>
                }
            />

            <Modal
                open={tabletFormOpen}
                onClose={() => setTabletFormOpen(false)}
                title={t("feedback.tablet.title")}
                size="xl"
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

            <Card title={t("feedback.surveysTitle")}>
                <div className="mb-4 flex flex-wrap items-end gap-4">
                    <Field label={t("feedback.filters.status")} className="min-w-40">
                        <Select value={surveyStatus} onChange={(event) => setSurveyStatus(event.target.value)}>
                            <option value="">{t("feedback.filters.allStatuses")}</option>
                            {SURVEY_STATUSES.map((status) => (
                                <option key={status} value={status}>
                                    {labels.enum(status)}
                                </option>
                            ))}
                        </Select>
                    </Field>
                </div>

                {surveysPending && <LoadingBlock label={t("feedback.loadingSurveys")} />}
                {surveysError && (
                    <LoadError
                        title={t("feedback.surveysError")}
                        onRetry={() => refetchSurveys()}
                        retrying={surveysFetching}
                    />
                )}
                {!surveysPending && !surveysError && <SurveysTable surveys={surveys ?? []} />}
            </Card>

            <Card title={t("feedback.responsesTitle")}>
                <div className="mb-4 flex flex-wrap items-end gap-4">
                    <Field label={t("feedback.filters.visitor")} className="min-w-48">
                        <Select
                            value={feedbackFilters.visitorId}
                            onChange={(event) => updateVisitorFilter(event.target.value)}
                        >
                            <option value="">{t("feedback.filters.allVisitors")}</option>
                            {(visitors ?? []).map((visitor) => (
                                <option key={visitor.id} value={visitor.id}>
                                    {visitor.fullName}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field label={t("feedback.filters.from")}>
                        <Input
                            type="date"
                            value={feedbackFilters.from}
                            onChange={(event) => updateDateFilter("from", event.target.value)}
                        />
                    </Field>

                    <Field label={t("feedback.filters.to")}>
                        <Input
                            type="date"
                            value={feedbackFilters.to}
                            onChange={(event) => updateDateFilter("to", event.target.value)}
                        />
                    </Field>

                    {hasFeedbackFilter && (
                        <Button variant="secondary" onClick={() => setFeedbackFilters(NO_FILTER)}>
                            {t("feedback.filters.clear")}
                        </Button>
                    )}

                    {incompleteRange && (
                        <p className="pb-2 text-xs text-text-muted">{t("feedback.filters.incompleteRange")}</p>
                    )}
                </div>

                {feedbackPending && <LoadingBlock label={t("feedback.loadingResponses")} />}
                {feedbackError && (
                    <LoadError
                        title={t("feedback.responsesError")}
                        onRetry={() => refetchFeedback()}
                        retrying={feedbackFetching}
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
                        <p className="mt-6 text-xs text-text-muted">{t("feedback.footnote")}</p>
                    </>
                )}
            </Card>
        </div>
    );
}
