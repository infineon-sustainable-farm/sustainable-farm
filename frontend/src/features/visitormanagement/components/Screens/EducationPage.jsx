import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
    useCreateTourStop,
    useCreateWorkshop,
    useDeactivateTourStop,
    useTourStops,
    useUpdateTourStop,
    useUpdateWorkshop,
    useWorkshopStatusAction,
    useWorkshops,
} from "../../hooks/useEducation";
import { useGuides } from "../../hooks/useScheduling";
import { Button, Card, Modal, PageHeader } from "../../../../shared/ui";
import { LoadError, LoadingBlock } from "../LoadState";
import TourStopsTimeline from "../TourStopsTimeline";
import TourStopForm from "../Forms/TourStopForm";
import WorkshopForm from "../Forms/WorkshopForm";
import WorkshopsTable from "../WorkshopsTable";

/** "Show inactive" switch of a section header. */
function ShowInactiveToggle({ checked, onChange }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <label className="flex items-center gap-2 text-sm text-text">
            <input
                type="checkbox"
                checked={checked}
                onChange={(event) => onChange(event.target.checked)}
                className="h-4 w-4 accent-primary"
            />
            {t("education.showInactive")}
        </label>
    );
}

export default function EducationPage() {
    const { t } = useTranslation("visitormanagement");
    const [formState, setFormState] = useState({ open: false, workshop: null });
    const [formKey, setFormKey] = useState(0);
    const [stopFormState, setStopFormState] = useState({ open: false, stop: null });
    const [stopFormKey, setStopFormKey] = useState(0);
    const [showInactiveStops, setShowInactiveStops] = useState(false);
    const [showInactiveWorkshops, setShowInactiveWorkshops] = useState(false);

    useEffect(() => {
        document.title = t("education.documentTitle");
    }, [t]);

    const {
        data: stops,
        isPending: stopsPending,
        isError: stopsError,
        refetch: refetchStops,
        isFetching: stopsFetching,
    } = useTourStops();
    const {
        data: workshops,
        isPending: workshopsPending,
        isError: workshopsError,
        refetch: refetchWorkshops,
        isFetching: workshopsFetching,
    } = useWorkshops();
    const { data: guides } = useGuides();

    const createMutation = useCreateWorkshop();
    const updateMutation = useUpdateWorkshop();
    const statusMutation = useWorkshopStatusAction();

    const createStopMutation = useCreateTourStop();
    const updateStopMutation = useUpdateTourStop();
    const deactivateStopMutation = useDeactivateTourStop();

    const editingWorkshop = formState.workshop;
    const formMutation = editingWorkshop ? updateMutation : createMutation;

    const editingStop = stopFormState.stop;
    const stopFormMutation = editingStop ? updateStopMutation : createStopMutation;

    // Only active stops make up the standard tour shown in the timeline.
    const activeStops = (stops ?? []).filter((stop) => stop.active);
    const inactiveStopCount = (stops ?? []).length - activeStops.length;

    // Deactivated workshops stay out of the table unless asked for.
    const visibleWorkshops = (workshops ?? []).filter(
        (workshop) => showInactiveWorkshops || workshop.status !== "INACTIVE",
    );

    function resetStopForm() {
        setStopFormState({ open: false, stop: null });
        setStopFormKey((current) => current + 1);
        createStopMutation.reset();
        updateStopMutation.reset();
    }

    function openCreateStop() {
        createStopMutation.reset();
        updateStopMutation.reset();
        setStopFormState({ open: true, stop: null });
        setStopFormKey((current) => current + 1);
    }

    function openEditStop(stop) {
        createStopMutation.reset();
        updateStopMutation.reset();
        setStopFormState({ open: true, stop });
        setStopFormKey((current) => current + 1);
    }

    function handleStopSubmit(values) {
        if (editingStop) {
            updateStopMutation.mutate(
                { id: editingStop.id, data: values },
                { onSuccess: resetStopForm },
            );
        } else {
            createStopMutation.mutate(values, { onSuccess: resetStopForm });
        }
    }

    const pendingStopDeactivateId = deactivateStopMutation.isPending
        ? deactivateStopMutation.variables
        : null;
    const stopDeactivateError = deactivateStopMutation.isError
        ? { id: deactivateStopMutation.variables, message: deactivateStopMutation.error.message }
        : null;

    function resetForm() {
        setFormState({ open: false, workshop: null });
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, workshop: null });
        setFormKey((current) => current + 1);
    }

    function openEdit(workshop) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, workshop });
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingWorkshop) {
            updateMutation.mutate({ id: editingWorkshop.id, data: values }, { onSuccess: resetForm });
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    const pendingAction = statusMutation.isPending ? statusMutation.variables : null;
    const actionError = statusMutation.isError
        ? { id: statusMutation.variables?.id, message: statusMutation.error.message }
        : null;

    return (
        <div className="p-6 space-y-6">
            <PageHeader title={t("education.title")} />

            {stopsPending && <LoadingBlock label={t("education.stops.loading")} />}

            {stopsError && (
                <LoadError
                    title={t("education.stops.loadError")}
                    onRetry={() => refetchStops()}
                    retrying={stopsFetching}
                />
            )}

            {!stopsPending && !stopsError && (
                <Card
                    title={t("education.stops.title")}
                    actions={
                        <>
                            <ShowInactiveToggle
                                checked={showInactiveStops}
                                onChange={setShowInactiveStops}
                            />
                            <Button icon={Plus} onClick={openCreateStop}>
                                {t("education.stops.add")}
                            </Button>
                        </>
                    }
                >
                    <TourStopsTimeline
                        stops={showInactiveStops ? (stops ?? []) : activeStops}
                        pendingDeactivateId={pendingStopDeactivateId}
                        deactivateError={stopDeactivateError}
                        onEdit={openEditStop}
                        onDeactivate={(stop) => deactivateStopMutation.mutate(stop.id)}
                    />

                    {!showInactiveStops && inactiveStopCount > 0 && (
                        <p className="mt-3 text-xs text-text-muted">
                            {inactiveStopCount === 1
                                ? t("education.stops.hiddenOne", { count: inactiveStopCount })
                                : t("education.stops.hiddenMany", { count: inactiveStopCount })}
                        </p>
                    )}
                </Card>
            )}

            <Card
                title={t("education.workshops.title")}
                actions={
                    <>
                        <ShowInactiveToggle
                            checked={showInactiveWorkshops}
                            onChange={setShowInactiveWorkshops}
                        />
                        <Button icon={Plus} onClick={openCreate}>
                            {t("education.workshops.add")}
                        </Button>
                    </>
                }
            >
                {workshopsPending && <LoadingBlock label={t("education.workshops.loading")} />}

                {workshopsError && (
                    <LoadError
                        title={t("education.workshops.loadError")}
                        onRetry={() => refetchWorkshops()}
                        retrying={workshopsFetching}
                    />
                )}

                {!workshopsPending && !workshopsError && (
                    <WorkshopsTable
                        workshops={visibleWorkshops}
                        pendingAction={pendingAction}
                        actionError={actionError}
                        onAction={(workshop, action) =>
                            statusMutation.mutate({ id: workshop.id, action })
                        }
                        onEdit={openEdit}
                    />
                )}
            </Card>

            {!workshopsPending && !workshopsError && (
                <p className="text-xs text-text-muted">{t("education.footnote")}</p>
            )}

            <Modal
                open={stopFormState.open}
                onClose={resetStopForm}
                size="xl"
                title={
                    editingStop
                        ? t("education.stops.editTitle", { name: editingStop.name })
                        : t("education.stops.newTitle")
                }
            >
                <TourStopForm
                    key={stopFormKey}
                    stop={editingStop}
                    isSubmitting={stopFormMutation.isPending}
                    submitError={stopFormMutation.error?.message}
                    serverFieldErrors={stopFormMutation.error?.data?.fieldErrors}
                    onSubmit={handleStopSubmit}
                    onCancel={resetStopForm}
                />
            </Modal>

            <Modal
                open={formState.open}
                onClose={resetForm}
                size="xl"
                title={
                    editingWorkshop
                        ? t("education.workshops.editTitle", { name: editingWorkshop.name })
                        : t("education.workshops.newTitle")
                }
            >
                <WorkshopForm
                    key={formKey}
                    workshop={editingWorkshop}
                    guides={guides ?? []}
                    isSubmitting={formMutation.isPending}
                    submitError={formMutation.error?.message}
                    serverFieldErrors={formMutation.error?.data?.fieldErrors}
                    onSubmit={handleSubmit}
                    onCancel={resetForm}
                />
            </Modal>
        </div>
    );
}
