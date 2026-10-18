import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import MaintenanceScheduleTable from "./MaintenanceScheduleTable";
import MaintenanceScheduleFormModal from "./MaintenanceScheduleFormModal";
import ConfirmDeleteRecordDialog from "./ConfirmDeleteRecordDialog";
import EmptyState from "../../../shared/components/EmptyState";
import ErrorState from "../../../shared/components/ErrorState";
import Pagination from "../../../shared/components/Pagination";
import { useMaintenanceSchedules } from "../hooks/useMaintenanceSchedules";
import { useEquipmentOptions } from "../hooks/useEquipmentOptions";
import {
  useCreateMaintenanceSchedule,
  useUpdateMaintenanceSchedule,
  useDeleteMaintenanceSchedule,
} from "../hooks/useMaintenanceScheduleMutations";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const PAGE_SIZE = 10;

function MaintenanceSchedule() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useMaintenanceSchedules(page, PAGE_SIZE);
  const equipmentOptions = useEquipmentOptions();

  const createMutation = useCreateMaintenanceSchedule();
  const updateMutation = useUpdateMaintenanceSchedule();
  const deleteMutation = useDeleteMaintenanceSchedule();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const equipments = useMemo(
    () => equipmentOptions.data?.content ?? [],
    [equipmentOptions.data]
  );
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  const activeFormMutation = editingSchedule ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return "—";
    return equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

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

  if (isLoading) return <div className="p-6 text-gray-500">Loading maintenance schedule…</div>;
  if (isError) {
    return (
      <ErrorState
        message="Failed to load the maintenance schedule. Please try again."
        onRetry={refetch}
      />
    );
  }

  const schedules = data.content;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold">Maintenance Schedule</h1>
        <button
          type="button"
          onClick={openCreateForm}
          className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
        >
          <Plus size={16} />
          Add maintenance schedule
        </button>
      </div>

      {schedules.length === 0 ? (
        <EmptyState message="No maintenance scheduled yet. Click “Add maintenance schedule” to get started." />
      ) : (
        <>
          <MaintenanceScheduleTable
            schedules={schedules}
            getEquipmentName={getEquipmentName}
            onEdit={openEditForm}
            onDelete={setDeleteTarget}
          />
          <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <MaintenanceScheduleFormModal
        key={isFormOpen ? (editingSchedule?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        schedule={editingSchedule}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDeleteRecordDialog
        isOpen={Boolean(deleteTarget)}
        title="Delete maintenance schedule"
        message={
          deleteTarget
            ? `Are you sure you want to delete the ${formatEnumLabel(deleteTarget.type).toLowerCase()} maintenance schedule for ${getEquipmentName(deleteTarget.equipmentId)}? This cannot be undone.`
            : ""
        }
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        serverError={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default MaintenanceSchedule;
