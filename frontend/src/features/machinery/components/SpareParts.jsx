import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import SparePartTable from "./SparePartTable";
import SparePartFormModal from "./SparePartFormModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useSpareParts } from "../hooks/useSpareParts";
import { useEquipmentLookup } from "../hooks/useEquipmentLookup";
import {
  useCreateSparePart,
  useUpdateSparePart,
  useDeleteSparePart,
} from "../hooks/useSparePartMutations";

const PAGE_SIZE = 10;

function SpareParts() {
  const { t } = useTranslation("machinery");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useSpareParts(page, PAGE_SIZE);
  const { equipments, isEquipmentLoading, getEquipmentName } = useEquipmentLookup();

  const createMutation = useCreateSparePart();
  const updateMutation = useUpdateSparePart();
  const deleteMutation = useDeleteSparePart();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSparePart, setEditingSparePart] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const activeFormMutation = editingSparePart ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

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

  function renderContent() {
    if (isLoading) return <SparePartTable loading getEquipmentName={getEquipmentName} />;
    if (isError) return <ErrorState message={t("spareParts.loadError")} />;
    if (data.content.length === 0) return <EmptyState message={t("spareParts.empty")} />;
    return (
      <>
        <SparePartTable
          spareParts={data.content}
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
        title={t("spareParts.title")}
        actions={<Button icon={Plus} onClick={openCreateForm}>{t("spareParts.add")}</Button>}
      />

      {renderContent()}

      <SparePartFormModal
        key={isFormOpen ? (editingSparePart?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        sparePart={editingSparePart}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t("spareParts.deleteTitle")}
        message={deleteTarget ? t("spareParts.deleteMessage", { name: deleteTarget.name }) : ""}
        onClose={closeDeleteDialog}
        onConfirm={() => handleDelete(deleteTarget.id)}
        loading={deleteMutation.isPending}
        error={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default SpareParts;
