import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ArrowUpRight, CheckCircle2 } from "lucide-react";
import { Button, Card, EmptyState, ErrorState, PageHeader, Skeleton } from "../../../shared/ui";
import ScreenAccess from "../components/ScreenAccess";
import PerimeterMap from "../components/PerimeterMap";
import { InlineAlert, LevelBadge, SensitivityMeter, StatusLabel } from "../components/ui";
import { useZones } from "../hooks/useZones";
import { useSiteSecurityAccess } from "../hooks/useSiteSecurityAccess";

const DEFAULT_ZONE = "restricted";

/* Zone texts are reference data: translated by zone id, the server value when no translation exists. */
function zoneText(t, zone, field) {
  return t(`zones.reference.${zone.id}.${field}`, { defaultValue: zone[field] ?? "" });
}

function ZonesContent() {
  const { t } = useTranslation("sitesecurity");
  const navigate = useNavigate();
  const { screens } = useSiteSecurityAccess();
  const [params, setParams] = useSearchParams();
  const selected = params.get("zone") ?? DEFAULT_ZONE;
  const { selectedZone: zone, isLoading, error } = useZones(selected);
  const select = (id) => setParams({ zone: id }, { replace: true });

  const header = <PageHeader title={t("zones.title")} subtitle={t("zones.subtitle")} />;
  if (isLoading) {
    return (
      <div className="space-y-5 p-6">
        {header}
        <Skeleton className="h-64 w-full" radius="card" />
      </div>
    );
  }
  if (error) {
    return (
      <div className="space-y-5 p-6">
        {header}
        <ErrorState message={t("zones.loadError")} />
      </div>
    );
  }
  if (!zone) {
    return (
      <div className="space-y-5 p-6">
        {header}
        <EmptyState message={t("zones.empty")} />
      </div>
    );
  }

  const requirements = zone.requirements ?? [];
  const name = zoneText(t, zone, "name");
  const tagline = zoneText(t, zone, "tagline");
  const description = zoneText(t, zone, "description");
  return (
    <div className="space-y-5 p-6">
      {header}
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <div className="space-y-3">
          <Card title={t("zones.diagram.title")} subtitle={t("zones.diagram.subtitle")}>
            <PerimeterMap selected={zone.id} onSelect={select} />
          </Card>
          <InlineAlert tone="warning">{t("zones.preliminary")}</InlineAlert>
        </div>

        <Card title={name} subtitle={tagline}>
          <div className="flex items-center gap-2.5">
            <LevelBadge level={zone.id} size="md" />
            <SensitivityMeter level={zone.id} />
          </div>
          <p className="mt-4 text-sm text-text-muted">{description}</p>
          <dl className="mt-5 grid grid-cols-2 gap-3">
            <div className="rounded-tile bg-surface-muted px-4 py-3">
              <dd className="text-3xl font-bold text-text">{zone.authorizedUsers}</dd>
              <dt className="text-xs font-medium uppercase tracking-wider text-text-muted">{t("zones.authorizedUsers")}</dt>
            </div>
            <div className="rounded-tile bg-surface-muted px-4 py-3">
              <dd className="text-3xl font-bold text-text">{zone.entryPoints}</dd>
              <dt className="text-xs font-medium uppercase tracking-wider text-text-muted">{t("zones.entryPoints")}</dt>
            </div>
          </dl>
          <div className="mt-3 flex items-center justify-between rounded-tile bg-surface-muted px-4 py-3">
            <span className="text-xs font-medium uppercase tracking-wider text-text-muted">{t("zones.status")}</span>
            <StatusLabel status={zone.status} />
          </div>
          <h4 className="mt-5 text-xs font-medium uppercase tracking-wider text-text-muted">{t("zones.requirements")}</h4>
          <ul className="mt-2 space-y-2">
            {requirements.map((requirement, index) => (
              <li key={requirement} className="flex items-start gap-2.5 text-sm text-text-muted">
                <CheckCircle2 aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-success-fg" />
                {t(`zones.reference.${zone.id}.requirements.${index}`, { defaultValue: requirement })}
              </li>
            ))}
          </ul>
          {screens.includes("log") && (
            <Button className="mt-5" fullWidth icon={ArrowUpRight} onClick={() => navigate(`/sitesecurity/log?zone=${zone.id}`)}>
              {t("zones.viewActivity")}
            </Button>
          )}
        </Card>
      </div>
    </div>
  );
}

export default function ZonesScreen() {
  return (
    <ScreenAccess screen="zones">
      <ZonesContent />
    </ScreenAccess>
  );
}
