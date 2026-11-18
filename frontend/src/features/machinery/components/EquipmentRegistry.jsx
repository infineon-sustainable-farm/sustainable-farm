import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import EquipmentTable from "./EquipmentTable";
import EquipmentFormModal from "./EquipmentFormModal";
import StatusUpdateModal from "./StatusUpdateModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useEquipments } from "../hooks/useEquipments";
import { useCreateEquipment, useUpdateEquipmentStatus, useDeleteEquipment } from "../hooks/useEquipmentMutations";

const PAGE_SIZE = 10;

function EquipmentRegistry() {
  const { t } = useTranslation("machinery");
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

  function renderContent() {
    if (isLoading) return <EquipmentTable loading />;
    if (isError) return <ErrorState message={t("equipment.loadError")} />;
    if (data.content.length === 0) return <EmptyState message={t("equipment.empty")} />;
    return (
      <>
        <EquipmentTable
          equipments={data.content}
          onEditStatus={setStatusTarget}
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
        title={t("equipment.title")}
        actions={
          <Button icon={Plus} onClick={() => { createMutation.reset(); setIsFormOpen(true); }}>
            {t("equipment.add")}
          </Button>
        }
      />

      {renderContent()}

      <EquipmentFormModal
        key={isFormOpen ? "open" : "closed"}
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleCreate}
        serverError={createMutation.isError ? createMutation.error.message : null}
      />

      <StatusUpdateModal
        key={statusTarget?.id ?? "none"}
        equipment={statusTarget}
        onClose={() => setStatusTarget(null)}
        onSubmit={handleUpdateStatus}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t("equipment.deleteTitle")}
        message={deleteTarget ? t("equipment.deleteMessage", { name: deleteTarget.name }) : ""}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => handleDelete(deleteTarget.id)}
        loading={deleteMutation.isPending}
      />
    </div>
  );
}
export default EquipmentRegistry;
