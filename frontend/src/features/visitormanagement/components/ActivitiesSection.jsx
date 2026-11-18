import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, Modal } from "../../../shared/ui";
import { useCreateActivity, useDeactivateActivity, useUpdateActivity } from "../hooks/useBooking";
import ActivitiesTable from "./ActivitiesTable";
import ActivityForm from "./Forms/ActivityForm";

/**
 * The activities of the booking screen: list with the inactive toggle, create and edit dialog,
 * and deactivation.
 */
export default function ActivitiesSection({ activities, activitiesError }) {
    const { t } = useTranslation("visitormanagement");
    const [formState, setFormState] = useState({ open: false, activity: null });
    const [formKey, setFormKey] = useState(0);
    const [showInactive, setShowInactive] = useState(false);

    const createMutation = useCreateActivity();
    const updateMutation = useUpdateActivity();
    const deactivateMutation = useDeactivateActivity();

    const editingActivity = formState.activity;
    const formMutation = editingActivity ? updateMutation : createMutation;

    function resetForm() {
        setFormState({ open: false, activity: null });
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openForm(activity) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, activity });
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingActivity) {
            updateMutation.mutate({ id: editingActivity.id, data: values }, { onSuccess: resetForm });
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    const pendingDeactivateId = deactivateMutation.isPending ? deactivateMutation.variables : null;
    const deactivateError = deactivateMutation.isError
        ? { id: deactivateMutation.variables, message: deactivateMutation.error.message }
        : null;

    // Inactive activities stay out of view unless asked for.
    const visibleActivities = (activities ?? []).filter((activity) => showInactive || activity.active);

    return (
        <Card
            title={t("booking.activities.title")}
            actions={
                <>
                    <label className="flex items-center gap-2 text-sm text-text">
                        <input
                            type="checkbox"
                            checked={showInactive}
                            onChange={(event) => setShowInactive(event.target.checked)}
                            className="h-4 w-4 accent-primary"
                        />
                        {t("booking.activities.showInactive")}
                    </label>
                    <Button icon={Plus} onClick={() => openForm(null)}>
                        {t("booking.activities.new")}
                    </Button>
                </>
            }
        >
            {activitiesError ? (
                <p className="text-sm text-text-muted">{t("booking.activities.loadError")}</p>
            ) : (
                <>
                    <ActivitiesTable
                        activities={visibleActivities}
                        pendingDeactivateId={pendingDeactivateId}
                        deactivateError={deactivateError}
                        onDeactivate={(activity) => deactivateMutation.mutate(activity.id)}
                        onEdit={openForm}
                    />
                    <p className="mt-3 text-xs text-text-muted">{t("booking.activities.note")}</p>
                </>
            )}

            <Modal
                open={formState.open}
                onClose={resetForm}
                size="xl"
                dismissible={!formMutation.isPending}
                title={
                    editingActivity
                        ? t("booking.activityForm.editTitle", { name: editingActivity.name })
                        : t("booking.activityForm.newTitle")
                }
            >
                <ActivityForm
                    key={formKey}
                    activity={editingActivity}
                    isSubmitting={formMutation.isPending}
                    submitError={formMutation.error?.message}
                    serverFieldErrors={formMutation.error?.data?.fieldErrors}
                    onSubmit={handleSubmit}
                    onCancel={resetForm}
                />
            </Modal>
        </Card>
    );
}
