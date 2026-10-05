import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import RepairLogTable from "./RepairLogTable";
import RepairLogFormModal from "./RepairLogFormModal";
import ConfirmDeleteRecordDialog from "./ConfirmDeleteRecordDialog";
import EmptyState from "../../../shared/components/EmptyState";
import ErrorState from "../../../shared/components/ErrorState";
import Pagination from "../../../shared/components/Pagination";
import { useRepairLogs } from "../hooks/useRepairLogs";
import { useEquipmentOptions } from "../hooks/useEquipmentOptions";
import {
  useCreateRepairLog,
  useUpdateRepairLog,
  useDeleteRepairLog,
} from "../hooks/useRepairLogMutations";
import { formatDate } from "../../../shared/utils/formatDate";

const PAGE_SIZE = 10;

function RepairLog() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useRepairLogs(page, PAGE_SIZE);
  const equipmentOptions = useEquipmentOptions();

  const createMutation = useCreateRepairLog();
  const updateMutation = useUpdateRepairLog();
  const deleteMutation = useDeleteRepairLog();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRepairLog, setEditingRepairLog] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const equipments = useMemo(
    () => equipmentOptions.data?.content ?? [],
    [equipmentOptions.data]
  );
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  const activeFormMutation = editingRepairLog ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return "—";
    return equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

  function openCreateForm() {
    createMutation.reset();
    updateMutation.reset();
    setEditingRepairLog(null);
    setIsFormOpen(true);
  }

  function openEditForm(repairLog) {
    createMutation.reset();
    updateMutation.reset();
    setEditingRepairLog(repairLog);
    setIsFormOpen(true);
  }

  function handleSubmit(values) {
    if (editingRepairLog) {
      updateMutation.mutate(
        { id: editingRepairLog.id, data: values },
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

  if (isLoading) return <div className="p-6 text-gray-500">Loading repair log…</div>;
  if (isError) {
    return (
      <ErrorState
        message="Failed to load the repair log. Please try again."
        onRetry={refetch}
      />
    );
  }

  const repairLogs = data.content;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold">Repair Log</h1>
        <button
          type="button"
          onClick={openCreateForm}
          className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
        >
          <Plus size={16} />
          Add repair log
        </button>
      </div>

      {repairLogs.length === 0 ? (
        <EmptyState message="No repairs logged yet. Click “Add repair log” to get started." />
      ) : (
        <>
          <RepairLogTable
            repairLogs={repairLogs}
            getEquipmentName={getEquipmentName}
            onEdit={openEditForm}
            onDelete={setDeleteTarget}
          />
          <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <RepairLogFormModal
        key={isFormOpen ? (editingRepairLog?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        repairLog={editingRepairLog}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDeleteRecordDialog
        isOpen={Boolean(deleteTarget)}
        title="Delete repair log"
        message={
          deleteTarget
            ? `Are you sure you want to delete the repair log of ${formatDate(deleteTarget.date)} for ${getEquipmentName(deleteTarget.equipmentId)}? This cannot be undone.`
            : ""
        }
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        serverError={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default RepairLog;
