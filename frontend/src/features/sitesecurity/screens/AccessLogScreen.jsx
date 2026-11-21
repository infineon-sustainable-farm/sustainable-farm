import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { Button, ErrorState, IconButton, PageHeader, Table } from "../../../shared/ui";
import ScreenAccess from "../components/ScreenAccess";
import LogDrawer from "../components/LogDrawer";
import { Avatar, FilterButton, LevelBadge, StatusLabel } from "../components/ui";
import { filterByZone } from "../utils/accessLogs";
import { translateEnum } from "../utils/status";
import { useAccessLogs } from "../hooks/useAccessLogs";

const FILTERS = ["all", "approved", "denied", "visitors", "staff", "service"];

function AccessLogContent() {
  const { t } = useTranslation("sitesecurity");
  const [params, setParams] = useSearchParams();
  const initialFilter = FILTERS.includes(params.get("filter")) ? params.get("filter") : "all";
  const { filter, setFilter, logs, allLogs, counts, isLoading, error } = useAccessLogs(initialFilter);
  const zone = params.get("zone");
  const [selected, setSelected] = useState(null);
  const rows = filterByZone(logs, zone);

  const clearZone = () => {
    const next = new URLSearchParams(params);
    next.delete("zone");
    setParams(next, { replace: true });
  };

  const columns = [
    { key: "time", header: t("log.columns.time"), cell: (e) => <span className="tabular-nums">{e.time}</span> },
    {
      key: "user",
      header: t("log.columns.person"),
      cell: (e) => (
        <div className="flex items-center gap-3">
          <Avatar initials={e.initials} level={e.personId ? e.zone : undefined} />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-text">{e.user ?? t("log.unidentified")}</p>
            <p className="text-xs text-text-muted">{e.ref}</p>
          </div>
        </div>
      ),
    },
    { key: "type", header: t("log.columns.type"), cell: (e) => translateEnum(t, "type", e.type) },
    { key: "zone", header: t("log.columns.zone"), cell: (e) => <LevelBadge level={e.zone} /> },
    { key: "action", header: t("log.columns.action"), cell: (e) => translateEnum(t, "action", e.action) },
    { key: "status", header: t("log.columns.status"), cell: (e) => <StatusLabel status={e.status} /> },
  ];

  return (
    <div className="space-y-5 p-6">
      <PageHeader title={t("log.title")} subtitle={t("log.subtitle")} meta={t("log.simulated")} />

      {error && <ErrorState message={t("log.loadError")} />}

      <div className="flex flex-wrap items-center gap-2" role="group" aria-label={t("log.filterLabel")}>
        {FILTERS.map((id) => (
          <FilterButton key={id} active={filter === id} onClick={() => setFilter(id)} count={counts[id]}>
            {t(`log.filters.${id}`)}
          </FilterButton>
        ))}
        {zone && (
          <span className="inline-flex items-center gap-2">
            <LevelBadge level={zone} />
            <Button variant="ghost" size="sm" onClick={clearZone}>
              {t("log.showAllZones")}
            </Button>
          </span>
        )}
        <span className="ml-auto text-xs text-text-muted">{t("log.count", { shown: rows.length, total: allLogs.length })}</span>
      </div>

      <Table
        caption={t("log.tableCaption")}
        columns={columns}
        rows={rows}
        getRowKey={(e) => e.id}
        loading={isLoading}
        emptyMessage={t("log.empty")}
        rowActions={(e) => (
          <IconButton icon={ChevronRight} label={t("log.openEvent", { ref: e.ref })} onClick={() => setSelected(e)} />
        )}
      />

      <LogDrawer entry={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

export default function AccessLogScreen() {
  return (
    <ScreenAccess screen="log">
      <AccessLogContent />
    </ScreenAccess>
  );
}
