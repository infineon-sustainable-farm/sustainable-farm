import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
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
import TourStopsTimeline from "../TourStopsTimeline";
import TourStopForm from "../Forms/TourStopForm";
import WorkshopForm from "../Forms/WorkshopForm";
import WorkshopsTable from "../WorkshopsTable";

export default function EducationPage() {
    const [formState, setFormState] = useState({ open: false, workshop: null });
    const [formKey, setFormKey] = useState(0);
    const [stopFormState, setStopFormState] = useState({ open: false, stop: null });
    const [stopFormKey, setStopFormKey] = useState(0);

    useEffect(() => {
        document.title = "Educational Program — Visitor Management";
    }, []);

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
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Educational program
                </h2>

                {stopsPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading tour stops…
                    </div>
                )}

                {stopsError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Tour stops could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetchStops()}
                            disabled={stopsFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {stopsFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!stopsPending && !stopsError && (
                    <>
                        <div className="mb-4 flex flex-wrap items-center gap-3">
                            <h3 className="font-heading text-[17px] font-bold text-ink">
                                Standard tour stops
                            </h3>
                            <button
                                type="button"
                                onClick={openCreateStop}
                                className="ml-auto rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                            >
                                + New stop
                            </button>
                        </div>

                        {stopFormState.open && (
                            <TourStopForm
                                key={stopFormKey}
                                stop={editingStop}
                                isSubmitting={stopFormMutation.isPending}
                                submitError={stopFormMutation.error?.message}
                                serverFieldErrors={stopFormMutation.error?.data?.fieldErrors}
                                onSubmit={handleStopSubmit}
                                onCancel={resetStopForm}
                            />
                        )}

                        <TourStopsTimeline
                            stops={activeStops}
                            pendingDeactivateId={pendingStopDeactivateId}
                            deactivateError={stopDeactivateError}
                            onEdit={openEditStop}
                            onDeactivate={(stop) => deactivateStopMutation.mutate(stop.id)}
                        />

                        {inactiveStopCount > 0 && (
                            <p className="mt-3 text-[11px] text-muted">
                                {inactiveStopCount} deactivated stop
                                {inactiveStopCount > 1 ? "s are" : " is"} hidden from the tour —
                                the API offers no reactivation.
                            </p>
                        )}
                    </>
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Workshops &amp; tour templates
                </h3>

                <div className="mb-4 flex gap-2.5">
                    <button
                        type="button"
                        onClick={openCreate}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New workshop
                    </button>
                </div>

                {formState.open && (
                    <WorkshopForm
                        key={formKey}
                        workshop={editingWorkshop}
                        isSubmitting={formMutation.isPending}
                        submitError={formMutation.error?.message}
                        serverFieldErrors={formMutation.error?.data?.fieldErrors}
                        onSubmit={handleSubmit}
                        onCancel={resetForm}
                    />
                )}

                {workshopsPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading workshops…
                    </div>
                )}

                {workshopsError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Workshops could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetchWorkshops()}
                            disabled={workshopsFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {workshopsFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!workshopsPending && !workshopsError && (
                    <>
                        <WorkshopsTable
                            workshops={workshops ?? []}
                            pendingAction={pendingAction}
                            actionError={actionError}
                            onAction={(workshop, action) =>
                                statusMutation.mutate({ id: workshop.id, action })
                            }
                            onEdit={openEdit}
                        />
                        <p className="mt-6 text-[11px] text-muted">
                            Tour stops define the standard visit, ordered by position. Workshops are
                            created as drafts and published once ready; deactivating a workshop keeps
                            its record but removes it from the offer.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}