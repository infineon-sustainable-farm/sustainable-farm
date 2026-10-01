import { useState } from "react";
import { Plus } from "lucide-react";
import EquipmentTable from "./EquipmentTable";
import EquipmentFormModal from "./EquipmentFormModal";
import StatusUpdateModal from "./StatusUpdateModal";
import ConfirmDeleteDialog from "./ConfirmDeleteDialog";
import EmptyState from "../../../shared/components/EmptyState";
import Pagination from "../../../shared/components/Pagination";
import { useEquipments } from "../hooks/useEquipments";
import { useCreateEquipment, useUpdateEquipmentStatus, useDeleteEquipment } from "../hooks/useEquipmentMutations";

const PAGE_SIZE = 20;

function EquipmentRegistry() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useEquipments(page);

  const createMutation = useCreateEquipment();
  const updateStatusMutation = useUpdateEquipmentStatus();
  const deleteMutation = useDeleteEquipment();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  function handleCreate(newEquipment) {
    createMutation.mutate(newEquipment, {
      onSuccess: () => setIsFormOpen(false),
    });
  }

  function handleUpdateStatus(id, status) {
    updateStatusMutation.mutate({ id, status }, {
      onSuccess: () => setStatusTarget(null),
    });
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

  if (isLoading) return <div className="p-6 text-gray-500">Loading equipment…</div>;
  if (isError) return <div className="p-6 text-red-600">Failed to load equipment. Please try again.</div>;

  const equipments = data.content;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold">Equipment Registry</h1>
        <button
          type="button"
          onClick={() => { createMutation.reset(); setIsFormOpen(true); }}
          className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
        >
          <Plus size={16} />
          Add equipment
        </button>
      </div>

      {equipments.length === 0 ? (
        <EmptyState message="No equipment registered yet. Click “Add equipment” to get started." />
      ) : (
        <>
          <EquipmentTable
            equipments={equipments}
            onEditStatus={setStatusTarget}
            onDelete={setDeleteTarget}
          />
          <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <EquipmentFormModal
        key={isFormOpen ? "open" : "closed"}
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleCreate}
        serverError={createMutation.isError ? createMutation.error.message : null}
      />

      <StatusUpdateModal
        equipment={statusTarget}
        onClose={() => setStatusTarget(null)}
        onSubmit={handleUpdateStatus}
      />

      <ConfirmDeleteDialog
        equipment={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
export default EquipmentRegistry;