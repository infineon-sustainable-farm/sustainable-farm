import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, RefreshCw, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { createComponent, deleteComponent, getComponents, updateComponent } from "../api/energyApi";
import { nextComponentId } from "../constants";
import { useEnergyFormat, useEnumLabels } from "../format";
import { Button, Card, ConfirmDialog, IconButton, PageHeader, Table } from "../../../shared/ui";
import ComponentFormModal from "./ComponentFormModal";
import InlineAlert from "./InlineAlert";

export default function EnergyComponents() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [components, setComponents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setComponents(await getComponents());
      setError("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getComponents()
      .then(setComponents)
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  async function save(values) {
    setSaving(true);
    try {
      if (editing) await updateComponent(editing.componentId, values);
      else await createComponent(values);
      setFormOpen(false);
      await refresh();
      return true;
    } catch (requestError) {
      setError(requestError.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function remove(component) {
    setSaving(true);
    try {
      await deleteComponent(component.componentId);
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  function confirmDelete() {
    const component = deleteTarget;
    setDeleteTarget(null);
    remove(component);
  }

  const nextId = nextComponentId(components);

  const columns = [
    { key: "componentId", header: t("components.id"), cell: (row) => <span className="font-semibold">{row.componentId}</span> },
    { key: "name", header: t("fields.name") },
    { key: "category", header: t("fields.category"), cell: (row) => labels.category(row.category) },
    {
      key: "capacityValue",
      header: t("fields.capacity"),
      cell: (row) => (row.capacityValue == null ? t("common.none") : format.number(row.capacityValue)),
    },
    { key: "status", header: t("fields.status"), cell: (row) => labels.status(row.status) },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("components.title")} subtitle={t("components.subtitle")} />

      {error && (
        <InlineAlert action={<IconButton icon={RefreshCw} label={t("actions.retry")} tone="danger" onClick={refresh} />}>
          {error}
        </InlineAlert>
      )}

      <Card
        title={t("components.registry")}
        actions={
          <Button
            icon={Plus}
            disabled={!nextId}
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            {t("components.add")}
          </Button>
        }
      >
        {components.length >= 5 && nextId && (
          <p className="mb-3 text-xs text-text-muted">{t("components.registryFull", { nextId })}</p>
        )}
        {loading ? (
          <p role="status" className="text-sm text-text-muted">{t("components.loading")}</p>
        ) : (
          <Table
            caption={t("components.registry")}
            columns={columns}
            rows={components}
            getRowKey={(row) => row.componentId}
            emptyMessage={t("components.empty")}
            rowActions={(component) => (
              <>
                <IconButton
                  icon={Pencil}
                  label={t("components.editLabel", { id: component.componentId })}
                  disabled={saving}
                  onClick={() => {
                    setEditing(component);
                    setFormOpen(true);
                  }}
                />
                <IconButton
                  icon={Trash2}
                  tone="danger"
                  label={t("components.deleteLabel", { id: component.componentId })}
                  disabled={saving}
                  onClick={() => setDeleteTarget(component)}
                />
              </>
            )}
          />
        )}
      </Card>

      {formOpen && (
        <ComponentFormModal
          component={editing}
          usedIds={components.map((c) => c.componentId)}
          error={error}
          saving={saving}
          onClose={() => setFormOpen(false)}
          onSave={save}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        message={deleteTarget ? t("components.deleteMessage", { id: deleteTarget.componentId }) : ""}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
      />
    </div>
  );
}
