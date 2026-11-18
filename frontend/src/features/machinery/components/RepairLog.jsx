import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import RepairLogTable from "./RepairLogTable";
import RepairLogFormModal from "./RepairLogFormModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useRepairLogs } from "../hooks/useRepairLogs";
import { useEquipmentLookup } from "../hooks/useEquipmentLookup";
import {
  useCreateRepairLog,
  useUpdateRepairLog,
  useDeleteRepairLog,
} from "../hooks/useRepairLogMutations";
import { useMachineryFormat } from "../format";

const PAGE_SIZE = 10;

function RepairLog() {
  const { t } = useTranslation("machinery");
  const format = useMachineryFormat();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useRepairLogs(page, PAGE_SIZE);
  const { equipments, isEquipmentLoading, getEquipmentName } = useEquipmentLookup();

  const createMutation = useCreateRepairLog();
  const updateMutation = useUpdateRepairLog();
  const deleteMutation = useDeleteRepairLog();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRepairLog, setEditingRepairLog] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const activeFormMutation = editingRepairLog ? updateMutation : createMutation;
  const formServerError = activeFormMutation.isError ? activeFormMutation.error.message : null;

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

  function renderContent() {
    if (isLoading) return <RepairLogTable loading getEquipmentName={getEquipmentName} />;
    if (isError) {
      return <ErrorState message={t("repair.loadError")} onRetry={refetch} retryLabel={t("common.tryAgain")} />;
    }
    if (data.content.length === 0) return <EmptyState message={t("repair.empty")} />;
    return (
      <>
        <RepairLogTable
          repairLogs={data.content}
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
        title={t("repair.title")}
        actions={<Button icon={Plus} onClick={openCreateForm}>{t("repair.add")}</Button>}
      />

      {renderContent()}

      <RepairLogFormModal
        key={isFormOpen ? (editingRepairLog?.id ?? "create") : "closed"}
        isOpen={isFormOpen}
        repairLog={editingRepairLog}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleSubmit}
        serverError={formServerError}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t("repair.deleteTitle")}
        message={
          deleteTarget
            ? t("repair.deleteMessage", {
                date: format.date(deleteTarget.date),
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
export default RepairLog;
