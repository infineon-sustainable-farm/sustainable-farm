import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import OperatorAssignmentTable from "./OperatorAssignmentTable";
import OperatorAssignmentFormModal from "./OperatorAssignmentFormModal";
import ConfirmDeleteOperatorAssignmentDialog from "./ConfirmDeleteOperatorAssignmentDialog";
import EmptyState from "../../../shared/components/EmptyState";
import Pagination from "../../../shared/components/Pagination";
import { useOperatorAssignments } from "../hooks/useOperatorAssignments";
import { useEquipmentOptions } from "../hooks/useEquipmentOptions";
import {
  useCreateOperatorAssignment,
  useUpdateOperatorAssignment,
  useDeleteOperatorAssignment,
} from "../hooks/useOperatorAssignmentMutations";

const PAGE_SIZE = 10;

function OperatorAssignments() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useOperatorAssignments(page);
  const equipmentOptions = useEquipmentOptions();

  const createMutation = useCreateOperatorAssignment();
  const updateMutation = useUpdateOperatorAssignment();
  const deleteMutation = useDeleteOperatorAssignment();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingAssignment, setEditingAssignment] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const equipments = useMemo(
    () => equipmentOptions.data?.content ?? [],
    [equipmentOptions.data]
  );
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  const activeFormMutation = editingAssignment ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

  function getEquipmentName(equipmentId) {
    return equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

  function openCreateForm() {
    createMutation.reset();
    updateMutation.reset();
    setEditingAssignment(null);
    setIsFormOpen(true);
  }

  function openEditForm(assignment) {
    createMutation.reset();
    updateMutation.reset();
    setEditingAssignment(assignment);
    setIsFormOpen(true);
  }

  function handleSubmit(values) {
    if (editingAssignment) {
      updateMutation.mutate(
        { id: editingAssignment.id, data: values },
        { onSuccess: () => setIsFormOpen(false) }
      );
      return;
    }
    createMutation.mutate(values, { onSuccess: () => setIsFormOpen(false) });
  }

  function handleDelete(id) {
    deleteMutation.mutate(id, {
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

  if (isLoading) return <div className="p-6 text-gray-500">Loading operator assignments…</div>;
  if (isError) return <div className="p-6 text-red-600">Failed to load operator assignments. Please try again.</div>;

  const assignments = data.content;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold">Operators</h1>
        <button
          type="button"
          onClick={openCreateForm}
          className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
        >
          <Plus size={16} />
          Assign operator
        </button>
      </div>

      {assignments.length === 0 ? (
        <EmptyState
          message="No operator assignments yet. Assign an operator to a piece of equipment to get started."
          actionLabel="Assign operator"
          onAction={openCreateForm}
        />
      ) : (
        <>
          <OperatorAssignmentTable
            assignments={assignments}
            getEquipmentName={getEquipmentName}
            onEdit={openEditForm}
            onDelete={setDeleteTarget}
          />
          <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <OperatorAssignmentFormModal
        key={isFormOpen ? (editingAssignment?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        assignment={editingAssignment}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDeleteOperatorAssignmentDialog
        assignment={deleteTarget}
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        serverError={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default OperatorAssignments;
