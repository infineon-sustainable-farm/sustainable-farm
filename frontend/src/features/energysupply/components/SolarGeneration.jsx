import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Sun, Trash2, TrendingUp, Zap } from "lucide-react";
import { useTranslation } from "react-i18next";
import { createSolarLog, deleteSolarLog, getAllSolarLogs, getComponents, updateSolarLog } from "../api/energyApi";
import { statusTone, useEnergyFormat, useEnumLabels } from "../format";
import { Button, Card, ConfirmDialog, ErrorState, IconButton, PageHeader, StatusBadge, Table } from "../../../shared/ui";
import EnergyKpiCard from "./EnergyKpiCard";
import InlineAlert from "./InlineAlert";
import SolarLogFormModal from "./SolarLogFormModal";

/** PV arrays with their latest log. */
function withLatestLog(components, solarLogs) {
  const latestByComponent = {};
  solarLogs.forEach((log) => {
    const existing = latestByComponent[log.componentId];
    if (log.componentId && (!existing || log.date > existing.date)) latestByComponent[log.componentId] = log;
  });
  return components
    .filter((component) => component.category === "PV_ARRAY")
    .map((component) => ({ ...component, latest: latestByComponent[component.componentId] || null }));
}

export default function SolarGeneration() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [arrays, setArrays] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [components, solarLogs] = await Promise.all([getComponents(), getAllSolarLogs()]);
      setLogs(solarLogs);
      setArrays(withLatestLog(components, solarLogs));
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([getComponents(), getAllSolarLogs()])
      .then(([components, solarLogs]) => {
        setLogs(solarLogs);
        setArrays(withLatestLog(components, solarLogs));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function save(values) {
    setSaving(true);
    setError(null);
    try {
      if (editing) await updateSolarLog(editing.logId, values);
      else await createSolarLog(values);
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
      await deleteSolarLog(log.logId);
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

  if (error && !arrays.length && !logs.length) {
    return (
      <div className="p-6">
        <ErrorState message={t("solar.loadError", { error })} />
      </div>
    );
  }
  if (loading) return <p role="status" className="p-6 text-sm text-text-muted">{t("solar.loading")}</p>;

  const totalCapacity = arrays.reduce((s, a) => s + (Number(a.capacityValue) || 0), 0);
  const totalYield = arrays.reduce((s, a) => s + (Number(a.latest?.kwhProduced) || 0), 0);
  const avgEfficiency = arrays.length
    ? arrays.reduce((s, a) => s + (Number(a.latest?.efficiencyPct) || 0), 0) / arrays.length
    : 0;

  const arrayColumns = [
    { key: "componentId", header: t("solar.componentId"), cell: (row) => <span className="font-semibold">{row.componentId}</span> },
    { key: "name", header: t("fields.name") },
    {
      key: "capacityValue",
      header: t("solar.capacityKwp"),
      className: "tabular-nums",
      cell: (row) => (row.capacityValue == null ? "" : format.fixed(row.capacityValue)),
    },
    {
      key: "latestOutput",
      header: t("solar.latestOutput"),
      className: "tabular-nums",
      cell: (row) => (row.latest ? format.fixed(row.latest.kwhProduced) : t("common.none")),
    },
    {
      key: "efficiency",
      header: t("solar.efficiency"),
      className: "tabular-nums",
      cell: (row) => (row.latest?.efficiencyPct == null ? t("common.none") : format.fixed(row.latest.efficiencyPct)),
    },
    {
      key: "status",
      header: t("fields.status"),
      cell: (row) => <StatusBadge tone={statusTone(row.status)} label={labels.status(row.status)} />,
    },
  ];

  const logColumns = [
    { key: "date", header: t("fields.date"), cell: (row) => <span className="font-normal">{format.date(row.date)}</span> },
    { key: "component", header: t("solar.pvComponent"), cell: (row) => row.componentName || row.componentId },
    { key: "kwhProduced", header: t("solar.kwhGenerated"), cell: (row) => format.number(row.kwhProduced) },
    {
      key: "efficiencyPct",
      header: t("solar.efficiency"),
      cell: (row) => (row.efficiencyPct == null ? t("common.none") : format.number(row.efficiencyPct)),
    },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("solar.title")} subtitle={t("solar.subtitle")} />

      {error && <InlineAlert>{error}</InlineAlert>}

      <div className="grid gap-4 sm:grid-cols-3">
        <EnergyKpiCard label={t("solar.totalCapacity")} value={format.fixed(totalCapacity)} unit={t("units.kwp")} icon={Sun} />
        <EnergyKpiCard label={t("solar.latestYield")} value={format.fixed(totalYield)} unit={t("units.kwh")} icon={Zap} />
        <EnergyKpiCard
          label={t("solar.averageEfficiency")}
          value={format.fixed(avgEfficiency)}
          unit={t("units.percentSign")}
          icon={TrendingUp}
        />
      </div>

      <Card title={t("solar.arraysTitle")}>
        <Table
          caption={t("solar.arraysTitle")}
          columns={arrayColumns}
          rows={arrays}
          getRowKey={(row) => row.componentId}
          emptyMessage={t("solar.noArrays")}
        />
      </Card>

      <Card
        title={t("solar.logsTitle")}
        actions={
          <Button
            icon={Plus}
            disabled={!arrays.length}
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            {t("solar.addLogButton")}
          </Button>
        }
      >
        <Table
          caption={t("solar.logsTitle")}
          columns={logColumns}
          rows={logs}
          getRowKey={(row) => row.logId}
          emptyMessage={t("solar.noLogs")}
          rowActions={(log) => (
            <>
              <IconButton
                icon={Pencil}
                label={t("solar.editLabel", { id: log.logId })}
                disabled={saving}
                onClick={() => {
                  setEditing(log);
                  setFormOpen(true);
                }}
              />
              <IconButton
                icon={Trash2}
                tone="danger"
                label={t("solar.deleteLabel", { id: log.logId })}
                disabled={saving}
                onClick={() => setDeleteTarget(log)}
              />
            </>
          )}
        />
      </Card>

      {formOpen && (
        <SolarLogFormModal
          log={editing}
          arrays={arrays}
          saving={saving}
          error={error}
          onClose={() => setFormOpen(false)}
          onSave={save}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        message={deleteTarget ? t("solar.deleteMessage", { date: format.date(deleteTarget.date) }) : ""}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  );
}
