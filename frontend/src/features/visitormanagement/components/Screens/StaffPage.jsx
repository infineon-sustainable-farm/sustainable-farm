import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useCreateStaff, useStaffList, useStaffStatusAction, useUpdateStaff } from "../../hooks/useStaff";
import { Button, ConfirmDialog, Field, Modal, PageHeader, Select } from "../../../../shared/ui";
import { useCreateParam } from "../../../../shared/hooks/useCreateParam";
import { useLabels } from "../../utils/labels";
import StaffForm from "../Forms/StaffForm";
import StaffTable from "../StaffTable";
import { LoadError, LoadingBlock } from "../LoadState";

const ROLES = ["GUIDE", "RECEPTION", "MANAGER"];
/** Role filter value of the people without a visitor role (farm workers). */
const NO_ROLE = "NONE";

export default function StaffPage() {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const [deactivateTarget, setDeactivateTarget] = useState(null);
    const [formState, setFormState] = useState({ open: false, staff: null });
    const [formKey, setFormKey] = useState(0);
    const [roleFilter, setRoleFilter] = useState("");
    const [showInactive, setShowInactive] = useState(false);
    // "?create=1": another module sent the user here to add a person (app/ownerLinks.js).
    const [createRequested, clearCreateRequest] = useCreateParam();
    const formOpen = formState.open || createRequested;

    useEffect(() => {
        document.title = t("staff.documentTitle");
    }, [t]);

    const {
        data: staff,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useStaffList();

    const createMutation = useCreateStaff();
    const updateMutation = useUpdateStaff();
    const statusMutation = useStaffStatusAction();

    const editingStaff = formState.staff;
    const formMutation = editingStaff ? updateMutation : createMutation;

    const visibleStaff = (staff ?? []).filter(
        (member) =>
            (roleFilter === "" ||
                (roleFilter === NO_ROLE ? !member.role : member.role === roleFilter)) &&
            (showInactive || member.active),
    );

    function resetForm() {
        clearCreateRequest();
        setFormState({ open: false, staff: null });
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, staff: null });
        setFormKey((current) => current + 1);
    }

    function openEdit(member) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, staff: member });
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingStaff) {
            updateMutation.mutate(
                { id: editingStaff.id, data: values },
                { onSuccess: resetForm },
            );
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    function requestDeactivate(member) {
        statusMutation.reset();
        setDeactivateTarget(member);
    }

    function confirmDeactivate() {
        const member = deactivateTarget;
        setDeactivateTarget(null);
        statusMutation.mutate({ staff: member, action: "deactivate" });
    }

    const pendingAction = statusMutation.isPending ? statusMutation.variables : null;
    const actionError = statusMutation.isError
        ? { id: statusMutation.variables?.staff?.id, message: statusMutation.error.message }
        : null;

    return (
        <div className="space-y-4 p-6">
            <PageHeader
                title={t("staff.title")}
                subtitle={t("staff.subtitle")}
                actions={
                    <Button icon={Plus} onClick={openCreate}>
                        {t("staff.new")}
                    </Button>
                }
            />

            <div className="flex flex-wrap items-end gap-4">
                <Field className="min-w-40" label={t("staff.roleFilter")}>
                    <Select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}>
                        <option value="">{t("staff.allRoles")}</option>
                        <option value={NO_ROLE}>{t("staff.noRole")}</option>
                        {ROLES.map((role) => (
                            <option key={role} value={role}>
                                {labels.enum(role)}
                            </option>
                        ))}
                    </Select>
                </Field>
                <label className="flex min-h-10 items-center gap-2 text-sm text-text">
                    <input
                        type="checkbox"
                        checked={showInactive}
                        onChange={(event) => setShowInactive(event.target.checked)}
                        className="h-4 w-4 accent-primary"
                    />
                    {t("staff.showInactive")}
                </label>
            </div>

            {isPending && <LoadingBlock label={t("staff.loading")} />}

            {isError && <LoadError title={t("staff.loadError")} onRetry={() => refetch()} retrying={isFetching} />}

            {!isPending && !isError && (
                <>
                    <StaffTable
                        staff={visibleStaff}
                        pendingAction={pendingAction}
                        actionError={actionError}
                        onAction={(member, action) => statusMutation.mutate({ staff: member, action })}
                        onDeactivate={requestDeactivate}
                        onEdit={openEdit}
                    />
                    <p className="text-xs text-text-muted">{t("staff.footnote")}</p>
                </>
            )}

            <Modal
                open={formOpen}
                onClose={resetForm}
                size="xl"
                dismissible={!formMutation.isPending}
                title={editingStaff ? t("staff.editTitle", { name: editingStaff.fullName }) : t("staff.new")}
            >
                {formOpen && (
                    <StaffForm
                        key={formKey}
                        staff={editingStaff}
                        isSubmitting={formMutation.isPending}
                        submitError={formMutation.error?.message}
                        serverFieldErrors={formMutation.error?.data?.fieldErrors}
                        onSubmit={handleSubmit}
                        onCancel={resetForm}
                    />
                )}
            </Modal>

            <ConfirmDialog
                open={Boolean(deactivateTarget)}
                title={t("staff.deactivateTitle")}
                message={deactivateTarget ? t("staff.deactivateMessage", { name: deactivateTarget.fullName }) : ""}
                confirmLabel={t("staff.yes")}
                cancelLabel={t("staff.no")}
                onClose={() => setDeactivateTarget(null)}
                onConfirm={confirmDeactivate}
            />
        </div>
    );
}
