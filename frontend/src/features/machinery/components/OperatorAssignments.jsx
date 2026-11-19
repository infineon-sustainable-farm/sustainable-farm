import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import OperatorAssignmentTable from "./OperatorAssignmentTable";
import OperatorAssignmentFormModal from "./OperatorAssignmentFormModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useOperatorAssignments } from "../hooks/useOperatorAssignments";
import { useEquipmentLookup } from "../hooks/useEquipmentLookup";
import {
  useCreateOperatorAssignment,
  useUpdateOperatorAssignment,
  useDeleteOperatorAssignment,
} from "../hooks/useOperatorAssignmentMutations";

const PAGE_SIZE = 10;

function OperatorAssignments() {
  const { t } = useTranslation("machinery");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useOperatorAssignments(page);
  const { equipments, isEquipmentLoading, getEquipmentName } = useEquipmentLookup();

  const createMutation = useCreateOperatorAssignment();
  const updateMutation = useUpdateOperatorAssignment();
  const deleteMutation = useDeleteOperatorAssignment();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingAssignment, setEditingAssignment] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const activeFormMutation = editingAssignment ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

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

  function renderContent() {
    if (isLoading) return <OperatorAssignmentTable loading getEquipmentName={getEquipmentName} />;
    if (isError) return <ErrorState message={t("operators.loadError")} />;
    if (data.content.length === 0) {
      return <EmptyState message={t("operators.empty")} actionLabel={t("operators.add")} onAction={openCreateForm} />;
    }
    return (
      <>
        <OperatorAssignmentTable
          assignments={data.content}
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
        title={t("operators.title")}
        actions={<Button icon={Plus} onClick={openCreateForm}>{t("operators.add")}</Button>}
      />

      {renderContent()}

      <OperatorAssignmentFormModal
        key={isFormOpen ? (editingAssignment?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        assignment={editingAssignment}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t("operators.deleteTitle")}
        message={deleteTarget ? t("operators.deleteMessage", { name: deleteTarget.fullName }) : ""}
        onClose={closeDeleteDialog}
        onConfirm={() => handleDelete(deleteTarget.id)}
        loading={deleteMutation.isPending}
        error={deleteMutation.isError ? deleteMutation.error.message : null}
      />
    </div>
  );
}
export default OperatorAssignments;
