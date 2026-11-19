import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import UsageLogTable from "./UsageLogTable";
import UsageLogFormModal from "./UsageLogFormModal";
import FuelLogTable from "./FuelLogTable";
import FuelLogFormModal from "./FuelLogFormModal";
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader, Pagination } from "../../../shared/ui";
import { useUsageLogs } from "../hooks/useUsageLogs";
import { useFuelLogs } from "../hooks/useFuelLogs";
import { useEquipmentLookup } from "../hooks/useEquipmentLookup";
import { useCreateUsageLog, useUpdateUsageLog, useDeleteUsageLog } from "../hooks/useUsageLogMutations";
import { useCreateFuelLog, useUpdateFuelLog, useDeleteFuelLog } from "../hooks/useFuelLogMutations";
import { useMachineryFormat } from "../format";

const PAGE_SIZE = 10;

/** One log of the page: heading with its add button, then loading, error, empty or the table. */
function LogSection({ title, addLabel, onAdd, query, page, onPageChange, loadError, emptyMessage, renderTable }) {
  const { t } = useTranslation("machinery");
  return (
    <section>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{title}</h2>
        <Button icon={Plus} onClick={onAdd}>{addLabel}</Button>
      </div>

      {query.isLoading && renderTable({ loading: true })}
      {query.isError && <ErrorState message={loadError} onRetry={query.refetch} retryLabel={t("common.tryAgain")} />}
      {query.isSuccess && query.data.content.length === 0 && <EmptyState message={emptyMessage} />}
      {query.isSuccess && query.data.content.length > 0 && (
        <>
          {renderTable({ rows: query.data.content })}
          <Pagination page={page} totalPages={query.data.totalPages} onPageChange={onPageChange} />
        </>
      )}
    </section>
  );
}

function UsageFuelLog() {
  const { t } = useTranslation("machinery");
  const format = useMachineryFormat();
  const [usagePage, setUsagePage] = useState(0);
  const [fuelPage, setFuelPage] = useState(0);

  const usageQuery = useUsageLogs(usagePage, PAGE_SIZE);
  const fuelQuery = useFuelLogs(fuelPage, PAGE_SIZE);
  const { equipments, isEquipmentLoading, getEquipmentName } = useEquipmentLookup();

  const createUsageMutation = useCreateUsageLog();
  const updateUsageMutation = useUpdateUsageLog();
  const deleteUsageMutation = useDeleteUsageLog();

  const createFuelMutation = useCreateFuelLog();
  const updateFuelMutation = useUpdateFuelLog();
  const deleteFuelMutation = useDeleteFuelLog();

  const [isUsageFormOpen, setIsUsageFormOpen] = useState(false);
  const [editingUsageLog, setEditingUsageLog] = useState(null);
  const [usageDeleteTarget, setUsageDeleteTarget] = useState(null);

  const [isFuelFormOpen, setIsFuelFormOpen] = useState(false);
  const [editingFuelLog, setEditingFuelLog] = useState(null);
  const [fuelDeleteTarget, setFuelDeleteTarget] = useState(null);

  function openCreateUsageForm() {
    createUsageMutation.reset();
    updateUsageMutation.reset();
    setEditingUsageLog(null);
    setIsUsageFormOpen(true);
  }

  function openEditUsageForm(usageLog) {
    createUsageMutation.reset();
    updateUsageMutation.reset();
    setEditingUsageLog(usageLog);
    setIsUsageFormOpen(true);
  }

  function handleUsageSubmit(values) {
    if (editingUsageLog) {
      updateUsageMutation.mutate(
        { id: editingUsageLog.id, data: values },
        { onSuccess: () => setIsUsageFormOpen(false) }
      );
      return;
    }
    createUsageMutation.mutate(values, { onSuccess: () => setIsUsageFormOpen(false) });
  }

  function handleUsageDelete() {
    deleteUsageMutation.mutate(usageDeleteTarget.id, {
      onSuccess: () => {
        const expectedTotalPages = Math.ceil((usageQuery.data.totalElements - 1) / PAGE_SIZE);
        if (usagePage > expectedTotalPages - 1 && expectedTotalPages > 0) setUsagePage(expectedTotalPages - 1);
        setUsageDeleteTarget(null);
      },
    });
  }

  function openCreateFuelForm() {
    createFuelMutation.reset();
    updateFuelMutation.reset();
    setEditingFuelLog(null);
    setIsFuelFormOpen(true);
  }

  function openEditFuelForm(fuelLog) {
    createFuelMutation.reset();
    updateFuelMutation.reset();
    setEditingFuelLog(fuelLog);
    setIsFuelFormOpen(true);
  }

  function handleFuelSubmit(values) {
    if (editingFuelLog) {
      updateFuelMutation.mutate(
        { id: editingFuelLog.id, data: values },
        { onSuccess: () => setIsFuelFormOpen(false) }
      );
      return;
    }
    createFuelMutation.mutate(values, { onSuccess: () => setIsFuelFormOpen(false) });
  }

  function handleFuelDelete() {
    deleteFuelMutation.mutate(fuelDeleteTarget.id, {
      onSuccess: () => {
        const expectedTotalPages = Math.ceil((fuelQuery.data.totalElements - 1) / PAGE_SIZE);
        if (fuelPage > expectedTotalPages - 1 && expectedTotalPages > 0) setFuelPage(expectedTotalPages - 1);
        setFuelDeleteTarget(null);
      },
    });
  }

  const activeUsageFormMutation = editingUsageLog ? updateUsageMutation : createUsageMutation;
  const activeFuelFormMutation = editingFuelLog ? updateFuelMutation : createFuelMutation;

  return (
    <div className="p-6">
      <PageHeader className="mb-6" title={t("usageFuel.title")} />

      <LogSection
        title={t("usage.title")}
        addLabel={t("usage.add")}
        onAdd={openCreateUsageForm}
        query={usageQuery}
        page={usagePage}
        onPageChange={setUsagePage}
        loadError={t("usage.loadError")}
        emptyMessage={t("usage.empty")}
        renderTable={({ rows, loading }) => (
          <UsageLogTable
            usageLogs={rows}
            loading={loading}
            getEquipmentName={getEquipmentName}
            onEdit={openEditUsageForm}
            onDelete={setUsageDeleteTarget}
          />
        )}
      />

      <hr className="my-8 border-border-divider" />

      <LogSection
        title={t("fuel.title")}
        addLabel={t("fuel.add")}
        onAdd={openCreateFuelForm}
        query={fuelQuery}
        page={fuelPage}
        onPageChange={setFuelPage}
        loadError={t("fuel.loadError")}
        emptyMessage={t("fuel.empty")}
        renderTable={({ rows, loading }) => (
          <FuelLogTable
            fuelLogs={rows}
            loading={loading}
            getEquipmentName={getEquipmentName}
            onEdit={openEditFuelForm}
            onDelete={setFuelDeleteTarget}
          />
        )}
      />

      <UsageLogFormModal
        key={isUsageFormOpen ? (editingUsageLog?.id ?? "create") : "closed"}
        isOpen={isUsageFormOpen}
        usageLog={editingUsageLog}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsUsageFormOpen(false)}
        onSubmit={handleUsageSubmit}
        serverError={activeUsageFormMutation.isError ? activeUsageFormMutation.error.message : null}
      />

      <FuelLogFormModal
        key={isFuelFormOpen ? (editingFuelLog?.id ?? "create") : "closed"}
        isOpen={isFuelFormOpen}
        fuelLog={editingFuelLog}
        equipments={equipments}
        isEquipmentLoading={isEquipmentLoading}
        onClose={() => setIsFuelFormOpen(false)}
        onSubmit={handleFuelSubmit}
        serverError={activeFuelFormMutation.isError ? activeFuelFormMutation.error.message : null}
      />

      <ConfirmDialog
        open={Boolean(usageDeleteTarget)}
        title={t("usage.deleteTitle")}
        message={
          usageDeleteTarget
            ? t("usage.deleteMessage", {
                date: format.date(usageDeleteTarget.date),
                equipment: getEquipmentName(usageDeleteTarget.equipmentId),
              })
            : ""
        }
        onClose={() => {
          deleteUsageMutation.reset();
          setUsageDeleteTarget(null);
        }}
        onConfirm={handleUsageDelete}
        loading={deleteUsageMutation.isPending}
        error={deleteUsageMutation.isError ? deleteUsageMutation.error.message : null}
      />

      <ConfirmDialog
        open={Boolean(fuelDeleteTarget)}
        title={t("fuel.deleteTitle")}
        message={
          fuelDeleteTarget
            ? t("fuel.deleteMessage", {
                date: format.date(fuelDeleteTarget.date),
                equipment: getEquipmentName(fuelDeleteTarget.equipmentId),
              })
            : ""
        }
        onClose={() => {
          deleteFuelMutation.reset();
          setFuelDeleteTarget(null);
        }}
        onConfirm={handleFuelDelete}
        loading={deleteFuelMutation.isPending}
        error={deleteFuelMutation.isError ? deleteFuelMutation.error.message : null}
      />
    </div>
  );
}
export default UsageFuelLog;
