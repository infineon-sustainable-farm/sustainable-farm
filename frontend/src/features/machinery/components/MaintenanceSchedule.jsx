import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import MaintenanceScheduleTable from "./MaintenanceScheduleTable";
import MaintenanceScheduleFormModal from "./MaintenanceScheduleFormModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useMaintenanceSchedules } from "../hooks/useMaintenanceSchedules";
import { useEquipmentLookup } from "../hooks/useEquipmentLookup";
import {
  useCreateMaintenanceSchedule,
  useUpdateMaintenanceSchedule,
  useDeleteMaintenanceSchedule,
} from "../hooks/useMaintenanceScheduleMutations";

const PAGE_SIZE = 10;

function MaintenanceSchedule() {
  const { t } = useTranslation("machinery");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useMaintenanceSchedules(page, PAGE_SIZE);
  const { equipments, isEquipmentLoading, getEquipmentName } = useEquipmentLookup();

  const createMutation = useCreateMaintenanceSchedule();
  const updateMutation = useUpdateMaintenanceSchedule();
  const deleteMutation = useDeleteMaintenanceSchedule();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const activeFormMutation = editingSchedule ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

  function openCreateForm() {
    createMutation.reset();
    updateMutation.reset();
    setEditingSchedule(null);
    setIsFormOpen(true);
  }

  function openEditForm(schedule) {
    createMutation.reset();
    updateMutation.reset();
    setEditingSchedule(schedule);
    setIsFormOpen(true);
  }

  function handleSubmit(values) {
    if (editingSchedule) {
      updateMutation.mutate(
        { id: editingSchedule.id, data: values },
        { onSuccess: () => setIsFormOpen(false) }
      );
      return;
    }
    createMutation.mutate(values, { onSuccess: () => setIsFormOpen(false) });
  }

  function handleDelete() {
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => {
        const expectedTotalPages = Math.ceil((data.totalElements - 1) / PAGE_SIZE);
        if (page > expectedTotalPages - 1 && expectedTotalPages > 0) setPage(expectedTotalPages - 1);
        setDeleteTarget(null);
      },
    });
  }

  function closeDeleteDialog() {
    deleteMutation.reset();
    setDeleteTarget(null);
  }

  function renderContent() {
    if (isLoading) return <MaintenanceScheduleTable loading getEquipmentName={getEquipmentName} />;
    if (isError) {
      return <ErrorState message={t("maintenance.loadError")} onRetry={refetch} retryLabel={t("common.tryAgain")} />;
    }
    if (data.content.length === 0) return <EmptyState message={t("maintenance.empty")} />;
    return (
      <>
        <MaintenanceScheduleTable
          schedules={data.content}
          getEquipmentName={getEquipmentName}
          onEdit={openEditForm}
          onDelete={setDeleteTarget}
        />
        <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
      </>
    );
  }

  return (
    <div className="p-6">
      <PageHeader
        className="mb-4"
        title={t("maintenance.title")}
        actions={<Button icon={Plus} onClick={openCreateForm}>{t("maintenance.add")}</Button>}
      />

      {renderContent()}

      <MaintenanceScheduleFormModal
        key={isFormOpen ? (editingSchedule?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        schedule={editingSchedule}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t("maintenance.deleteTitle")}
        message={
          deleteTarget
            ? t(`maintenance.deleteMessage.${deleteTarget.type}`, {
                equipment: getEquipmentName(deleteTarget.equipmentId),
              })
            : ""
        }
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        loading={deleteMutation.isPending}
        error={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default MaintenanceSchedule;
