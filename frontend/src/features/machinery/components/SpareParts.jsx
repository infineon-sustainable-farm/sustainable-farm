import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import SparePartTable from "./SparePartTable";
import SparePartFormModal from "./SparePartFormModal";
import ConfirmDeleteSparePartDialog from "./ConfirmDeleteSparePartDialog";
import EmptyState from "../../../shared/components/EmptyState";
import Pagination from "../../../shared/components/Pagination";
import { useSpareParts } from "../hooks/useSpareParts";
import { useEquipmentOptions } from "../hooks/useEquipmentOptions";
import {
  useCreateSparePart,
  useUpdateSparePart,
  useDeleteSparePart,
} from "../hooks/useSparePartMutations";

const PAGE_SIZE = 10;

function SpareParts() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useSpareParts(page, PAGE_SIZE);
  const equipmentOptions = useEquipmentOptions();

  const createMutation = useCreateSparePart();
  const updateMutation = useUpdateSparePart();
  const deleteMutation = useDeleteSparePart();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSparePart, setEditingSparePart] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const equipments = useMemo(
    () => equipmentOptions.data?.content ?? [],
    [equipmentOptions.data]
  );
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  const activeFormMutation = editingSparePart ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return "—";
    return equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

  function openCreateForm() {
    createMutation.reset();
    updateMutation.reset();
    setEditingSparePart(null);
    setIsFormOpen(true);
  }

  function openEditForm(sparePart) {
    createMutation.reset();
    updateMutation.reset();
    setEditingSparePart(sparePart);
    setIsFormOpen(true);
  }

  function handleSubmit(values) {
    if (editingSparePart) {
      updateMutation.mutate(
        { id: editingSparePart.id, data: values },
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

  if (isLoading) return <div className="p-6 text-gray-500">Loading spare parts…</div>;
  if (isError) return <div className="p-6 text-red-600">Failed to load spare parts. Please try again.</div>;

  const spareParts = data.content;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold">Spare Parts</h1>
        <button
          type="button"
          onClick={openCreateForm}
          className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
        >
          <Plus size={16} />
          Add spare part
        </button>
      </div>

      {spareParts.length === 0 ? (
        <EmptyState message="No spare parts registered yet. Click “Add spare part” to get started." />
      ) : (
        <>
          <SparePartTable
            spareParts={spareParts}
            getEquipmentName={getEquipmentName}
            onEdit={openEditForm}
            onDelete={setDeleteTarget}
          />
          <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}

      <SparePartFormModal
        key={isFormOpen ? (editingSparePart?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        sparePart={editingSparePart}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDeleteSparePartDialog
        sparePart={deleteTarget}
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        serverError={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default SpareParts;
