import { useEffect, useState } from "react";
import { Clock, Fuel, Pencil, Plus, Power, RefreshCw, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getComponents } from "../api/energyApi";
import { useGeneratorEvents } from "../hooks/useGeneratorEvents";
import { useEnergyFormat, useEnumLabels } from "../format";
import { Button, Card, ConfirmDialog, IconButton, PageHeader, Table } from "../../../shared/ui";
import EnergyKpiCard from "./EnergyKpiCard";
import GeneratorEventFormModal from "./GeneratorEventFormModal";
import InlineAlert from "./InlineAlert";

export default function GeneratorBackup() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [generator, setGenerator] = useState(null);
  const [generatorError, setGeneratorError] = useState(null);
  const [generatorLoading, setGeneratorLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const { events, summary, loading, saving, error, refresh, saveEvent, removeEvent } = useGeneratorEvents();

  useEffect(() => {
    let active = true;
    getComponents()
      .then((components) => {
        if (active) setGenerator(components.find((component) => component.category === "GENERATOR") || null);
      })
      .catch((requestError) => {
        if (active) setGeneratorError(requestError.message);
      })
      .finally(() => {
        if (active) setGeneratorLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading || generatorLoading) {
    return <p role="status" className="p-6 text-sm text-text-muted">{t("generator.loading")}</p>;
  }

  function confirmDelete() {
    const event = deleteTarget;
    setDeleteTarget(null);
    removeEvent(event.eventId).catch(() => {});
  }

  const notRecorded = (value, render) => (value == null ? t("generator.notRecorded") : render(value));

  const columns = [
    {
      key: "date",
      header: t("fields.date"),
      className: "whitespace-nowrap text-text-muted",
      cell: (row) => <span className="font-normal">{format.date(row.date)}</span>,
    },
    { key: "triggerReason", header: t("generator.triggerReason"), cell: (row) => row.triggerReason || t("common.none") },
    {
      key: "durationHours",
      header: t("generator.durationHrs"),
      className: "tabular-nums",
      cell: (row) => notRecorded(row.durationHours, (value) => format.fixed(value)),
    },
    {
      key: "fuelLiters",
      header: t("generator.fuelL"),
      className: "tabular-nums",
      cell: (row) => notRecorded(row.fuelLiters, (value) => format.fixed(value)),
    },
    {
      key: "costEur",
      header: t("generator.costEur"),
      className: "font-medium tabular-nums",
      cell: (row) => notRecorded(row.costEur, (value) => t("units.euro", { value: format.fixed(value, 2) })),
    },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("generator.title")} subtitle={t("generator.subtitle")} />

      {generatorError && <InlineAlert>{t("generator.componentLoadError", { error: generatorError })}</InlineAlert>}
      {error && (
        <InlineAlert
          action={
            <Button variant="ghost" size="sm" icon={RefreshCw} onClick={() => refresh().catch(() => {})}>
              {t("actions.retry")}
            </Button>
          }
        >
          {t("generator.eventsError", { error })}
        </InlineAlert>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <EnergyKpiCard
          label={t("generator.capacity")}
          value={generator?.capacityValue == null ? t("common.none") : format.number(generator.capacityValue)}
          unit={t("units.kw")}
          icon={Power}
        />
        <EnergyKpiCard label={t("generator.currentStatus")} value={labels.status(generator?.status)} icon={Power} />
        <EnergyKpiCard
          label={t("generator.runtimeMonth")}
          value={format.fixed(Number(summary?.runtimeHoursThisMonth ?? 0))}
          unit={t("units.hrs")}
          icon={Clock}
        />
        <EnergyKpiCard
          label={t("generator.fuelMonth")}
          value={format.fixed(Number(summary?.fuelLitersThisMonth ?? 0))}
          unit={t("units.liters")}
          icon={Fuel}
        />
      </div>

      <Card
        title={t("generator.eventsTitle")}
        actions={
          <Button
            icon={Plus}
            onClick={() => {
              setEditingEvent(null);
              setFormOpen(true);
            }}
          >
            {t("generator.addEventButton")}
          </Button>
        }
      >
        <Table
          caption={t("generator.eventsTitle")}
          columns={columns}
          rows={events}
          getRowKey={(row) => row.eventId}
          emptyMessage={t("generator.noEvents")}
          rowActions={(event) => (
            <>
              <IconButton
                icon={Pencil}
                label={t("generator.editLabel", { date: format.date(event.date) })}
                disabled={saving}
                onClick={() => {
                  setEditingEvent(event);
                  setFormOpen(true);
                }}
              />
              <IconButton
                icon={Trash2}
                tone="danger"
                label={t("generator.deleteLabel", { date: format.date(event.date) })}
                disabled={saving}
                onClick={() => setDeleteTarget(event)}
              />
            </>
          )}
        />
      </Card>

      {formOpen && (
        <GeneratorEventFormModal
          event={editingEvent}
          saving={saving}
          error={error}
          onClose={() => setFormOpen(false)}
          onSave={saveEvent}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        message={deleteTarget ? t("generator.deleteMessage", { date: format.date(deleteTarget.date) }) : ""}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  );
}
