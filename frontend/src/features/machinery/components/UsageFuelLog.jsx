import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import UsageLogTable from "./UsageLogTable";
import UsageLogFormModal from "./UsageLogFormModal";
import FuelLogTable from "./FuelLogTable";
import FuelLogFormModal from "./FuelLogFormModal";
import ConfirmDeleteRecordDialog from "./ConfirmDeleteRecordDialog";
import EmptyState from "../../../shared/components/EmptyState";
import ErrorState from "../../../shared/components/ErrorState";
import Pagination from "../../../shared/components/Pagination";
import { useUsageLogs } from "../hooks/useUsageLogs";
import { useFuelLogs } from "../hooks/useFuelLogs";
import { useEquipmentOptions } from "../hooks/useEquipmentOptions";
import { useCreateUsageLog, useUpdateUsageLog, useDeleteUsageLog } from "../hooks/useUsageLogMutations";
import { useCreateFuelLog, useUpdateFuelLog, useDeleteFuelLog } from "../hooks/useFuelLogMutations";
import { formatDate } from "../../../shared/utils/formatDate";

const PAGE_SIZE = 10;

function UsageFuelLog() {
  const [usagePage, setUsagePage] = useState(0);
  const [fuelPage, setFuelPage] = useState(0);

  const usageQuery = useUsageLogs(usagePage, PAGE_SIZE);
  const fuelQuery = useFuelLogs(fuelPage, PAGE_SIZE);
  const equipmentOptions = useEquipmentOptions();

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

  const equipments = useMemo(
    () => equipmentOptions.data?.content ?? [],
    [equipmentOptions.data]
  );
  const equipmentNames = useMemo(
    () => new Map(equipments.map((equipment) => [equipment.id, equipment.name])),
    [equipments]
  );

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return "—";
    return equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

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
      <h1 className="text-xl font-bold mb-6">Usage &amp; Fuel Log</h1>

      <section>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold">Usage Log</h2>
          <button
            type="button"
            onClick={openCreateUsageForm}
            className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
          >
            <Plus size={16} />
            Add usage log
          </button>
        </div>

        {usageQuery.isLoading && <p className="text-gray-500">Loading usage log…</p>}
        {usageQuery.isError && (
          <ErrorState
            message="Failed to load the usage log. Please try again."
            onRetry={usageQuery.refetch}
          />
        )}
        {usageQuery.isSuccess && usageQuery.data.content.length === 0 && (
          <EmptyState message="No usage logged yet. Click “Add usage log” to get started." />
        )}
        {usageQuery.isSuccess && usageQuery.data.content.length > 0 && (
          <>
            <UsageLogTable
              usageLogs={usageQuery.data.content}
              getEquipmentName={getEquipmentName}
              onEdit={openEditUsageForm}
              onDelete={setUsageDeleteTarget}
            />
            <Pagination page={usagePage} totalPages={usageQuery.data.totalPages} onPageChange={setUsagePage} />
          </>
        )}
      </section>

      <div className="border-t border-gray-200 my-8" />

      <section>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold">Fuel Log</h2>
          <button
            type="button"
            onClick={openCreateFuelForm}
            className="bg-primary text-white px-4 py-2 rounded-lg flex items-center gap-2"
          >
            <Plus size={16} />
            Add fuel log
          </button>
        </div>

        {fuelQuery.isLoading && <p className="text-gray-500">Loading fuel log…</p>}
        {fuelQuery.isError && (
          <ErrorState
            message="Failed to load the fuel log. Please try again."
            onRetry={fuelQuery.refetch}
          />
        )}
        {fuelQuery.isSuccess && fuelQuery.data.content.length === 0 && (
          <EmptyState message="No fuel logged yet. Click “Add fuel log” to get started." />
        )}
        {fuelQuery.isSuccess && fuelQuery.data.content.length > 0 && (
          <>
            <FuelLogTable
              fuelLogs={fuelQuery.data.content}
              getEquipmentName={getEquipmentName}
              onEdit={openEditFuelForm}
              onDelete={setFuelDeleteTarget}
            />
            <Pagination page={fuelPage} totalPages={fuelQuery.data.totalPages} onPageChange={setFuelPage} />
          </>
        )}
      </section>

      <UsageLogFormModal
        key={isUsageFormOpen ? (editingUsageLog?.id ?? "create") : "closed"}
        isOpen={isUsageFormOpen}
        usageLog={editingUsageLog}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsUsageFormOpen(false)}
        onSubmit={handleUsageSubmit}
        serverError={activeUsageFormMutation.isError ? activeUsageFormMutation.error.message : null}
      />

      <FuelLogFormModal
        key={isFuelFormOpen ? (editingFuelLog?.id ?? "create") : "closed"}
        isOpen={isFuelFormOpen}
        fuelLog={editingFuelLog}
        equipments={equipments}
        isEquipmentLoading={equipmentOptions.isLoading}
        onClose={() => setIsFuelFormOpen(false)}
        onSubmit={handleFuelSubmit}
        serverError={activeFuelFormMutation.isError ? activeFuelFormMutation.error.message : null}
      />

      <ConfirmDeleteRecordDialog
        isOpen={Boolean(usageDeleteTarget)}
        title="Delete usage log"
        message={
          usageDeleteTarget
            ? `Are you sure you want to delete the usage log of ${formatDate(usageDeleteTarget.date)} for ${getEquipmentName(usageDeleteTarget.equipmentId)}? This cannot be undone.`
            : ""
        }
        onClose={() => {
          deleteUsageMutation.reset();
          setUsageDeleteTarget(null);
        }}
        onConfirm={handleUsageDelete}
        serverError={deleteUsageMutation.isError ? deleteUsageMutation.error.message : null}
      />

      <ConfirmDeleteRecordDialog
        isOpen={Boolean(fuelDeleteTarget)}
        title="Delete fuel log"
        message={
          fuelDeleteTarget
            ? `Are you sure you want to delete the fuel log of ${formatDate(fuelDeleteTarget.date)} for ${getEquipmentName(fuelDeleteTarget.equipmentId)}? This cannot be undone.`
            : ""
        }
        onClose={() => {
          deleteFuelMutation.reset();
          setFuelDeleteTarget(null);
        }}
        onConfirm={handleFuelDelete}
        serverError={deleteFuelMutation.isError ? deleteFuelMutation.error.message : null}
      />
    </div>
  );
}
export default UsageFuelLog;
