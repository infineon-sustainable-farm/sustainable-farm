import { useCallback, useEffect, useState } from "react";
import { Gauge, ListChecks, Pencil, Plus, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  createConsumptionLog,
  deleteConsumptionLog,
  getAllConsumption,
  getConsumptionToday,
  updateConsumptionLog,
} from "../api/energyApi";
import { useEnergyFormat, useEnumLabels } from "../format";
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, IconButton, PageHeader, Table } from "../../../shared/ui";
import ConsumptionDonut from "./ConsumptionDonut";
import ConsumptionFormModal from "./ConsumptionFormModal";
import EnergyKpiCard from "./EnergyKpiCard";
import InlineAlert from "./InlineAlert";

export default function ConsumptionTracking() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [logs, setLogs] = useState(null);
  const [allLogs, setAllLogs] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const refresh = useCallback(async () => {
    try {
      const [today, all] = await Promise.all([getConsumptionToday(), getAllConsumption()]);
      setLogs(today);
      setAllLogs(all);
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    Promise.all([getConsumptionToday(), getAllConsumption()])
      .then(([today, all]) => {
        setLogs(today);
        setAllLogs(all);
      })
      .catch((err) => setError(err.message));
  }, []);

  async function save(values) {
    setSaving(true);
    setError(null);
    try {
      if (editing) await updateConsumptionLog(editing.logId, values);
      else await createConsumptionLog(values);
      setFormOpen(false);
      await refresh();
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function remove(log) {
    setSaving(true);
    setError(null);
    try {
      await deleteConsumptionLog(log.logId);
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function confirmDelete() {
    const log = deleteTarget;
    setDeleteTarget(null);
    remove(log);
  }

  if (error && !logs) {
    return (
      <div className="p-6">
        <ErrorState message={t("consumption.loadError", { error })} />
      </div>
    );
  }
  if (!logs) return <p role="status" className="p-6 text-sm text-text-muted">{t("consumption.loading")}</p>;

  const total = logs.reduce((s, l) => s + (l.kwhConsumed || 0), 0);
  const share = (value) => t("units.percent", { value: format.number(total ? Math.round((value / total) * 100) : 0) });

  const todayColumns = [
    { key: "loadCategory", header: t("fields.category"), cell: (row) => labels.loadCategory(row.loadCategory) },
    { key: "kwhConsumed", header: t("consumption.kwhTodayHeader"), className: "tabular-nums", cell: (row) => format.number(row.kwhConsumed) },
    { key: "share", header: t("consumption.shareOfTotal"), className: "tabular-nums", cell: (row) => share(row.kwhConsumed) },
    { key: "source", header: t("fields.source"), cell: (row) => labels.source(row.source) },
  ];

  const logColumns = [
    { key: "date", header: t("fields.date"), cell: (row) => <span className="font-normal">{format.date(row.date)}</span> },
    { key: "loadCategory", header: t("fields.category"), cell: (row) => labels.loadCategory(row.loadCategory) },
    { key: "kwhConsumed", header: t("units.kwh"), cell: (row) => format.number(row.kwhConsumed) },
    { key: "source", header: t("fields.source"), cell: (row) => labels.source(row.source) },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("consumption.title")} subtitle={t("consumption.subtitle")} />

      {error && <InlineAlert>{error}</InlineAlert>}

      <div className="grid gap-4 sm:grid-cols-2">
        <EnergyKpiCard label={t("consumption.totalToday")} value={format.number(total)} unit={t("units.kwh")} icon={Gauge} />
        <EnergyKpiCard label={t("consumption.categoriesReporting")} value={format.number(logs.length)} icon={ListChecks} />
      </div>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3" title={t("consumption.byCategoryTitle")}>
          <Table
            caption={t("consumption.byCategoryTitle")}
            columns={todayColumns}
            rows={logs}
            getRowKey={(row) => row.logId}
            emptyMessage={t("consumption.noDataToday")}
          />
        </Card>
        <Card className="xl:col-span-2" title={t("consumption.breakdownTitle")}>
          {logs.length > 0 ? (
            <ConsumptionDonut data={logs} total={total} />
          ) : (
            <EmptyState message={t("consumption.noDataToday")} />
          )}
        </Card>
      </div>

      <Card
        title={t("consumption.logsTitle")}
        actions={
          <Button
            icon={Plus}
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            {t("consumption.addLogButton")}
          </Button>
        }
      >
        <Table
          caption={t("consumption.logsTitle")}
          columns={logColumns}
          rows={allLogs}
          getRowKey={(row) => row.logId}
          emptyMessage={t("consumption.noLogs")}
          rowActions={(log) => (
            <>
              <IconButton
                icon={Pencil}
                label={t("consumption.editLabel", { id: log.logId })}
                disabled={saving}
                onClick={() => {
                  setEditing(log);
                  setFormOpen(true);
                }}
              />
              <IconButton
                icon={Trash2}
                tone="danger"
                label={t("consumption.deleteLabel", { id: log.logId })}
                disabled={saving}
                onClick={() => setDeleteTarget(log)}
              />
            </>
          )}
        />
      </Card>

      {formOpen && (
        <ConsumptionFormModal
          log={editing}
          saving={saving}
          error={error}
          onClose={() => setFormOpen(false)}
          onSave={save}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        message={deleteTarget ? t("consumption.deleteMessage", { date: format.date(deleteTarget.date) }) : ""}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  );
}
