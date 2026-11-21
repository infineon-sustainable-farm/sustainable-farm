import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, ArrowUpRight, DoorOpen, Layers3, UserRoundCheck } from "lucide-react";
import { Button, Card, EmptyState, ErrorState, KpiCard, PageHeader, Skeleton } from "../../../shared/ui";
import ScreenAccess from "../components/ScreenAccess";
import PerimeterMap from "../components/PerimeterMap";
import RecentActivity from "../components/RecentActivity";
import { LevelBadge, StatusLabel } from "../components/ui";
import { LEVEL_ORDER } from "../api/sitesecurityTypes";
import { useSecurityOverview } from "../hooks/useSecurityOverview";
import { useSiteSecurityAccess } from "../hooks/useSiteSecurityAccess";
import { accessLabelKey } from "../utils/accessRights";
import { buildStatCards } from "../utils/dashboardStats";

const STAT_STYLE = {
  points: { icon: DoorOpen, tone: "primary" },
  zones: { icon: Layers3, tone: "info" },
  visitors: { icon: UserRoundCheck, tone: "primary" },
  denied: { icon: AlertTriangle, tone: "error" },
};

function DashboardContent() {
  const { t } = useTranslation("sitesecurity");
  const navigate = useNavigate();
  const { authorization, screens } = useSiteSecurityAccess();
  const { gates, stats, isLoading, error } = useSecurityOverview();
  const canOpenZones = screens.includes("zones");
  const canViewLog = screens.includes("log");
  const cards = buildStatCards(stats, gates.length);

  const openZones = (zone) => canOpenZones && navigate(zone ? `/sitesecurity/zones?zone=${zone}` : "/sitesecurity/zones");
  const openLog = (filter) => canViewLog && navigate(filter ? `/sitesecurity/log?filter=${filter}` : "/sitesecurity/log");

  return (
    <div className="space-y-5 p-6">
      <PageHeader title={t("dashboard.title")} subtitle={t("dashboard.subtitle")} meta={t(accessLabelKey(authorization))} />

      {error && <ErrorState message={t("dashboard.loadError")} />}

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {cards.map((card) => {
          const style = STAT_STYLE[card.id];
          return (
            <KpiCard
              key={card.id}
              icon={style.icon}
              tone={card.alert ? "error" : style.tone}
              label={t(card.labelKey)}
              value={card.value}
              hint={card.hintKey ? t(card.hintKey, card.hintValues) : undefined}
              to={card.alert && canViewLog ? "/sitesecurity/log?filter=denied" : undefined}
            />
          );
        })}
      </div>

      <Card
        title={t("dashboard.perimeter.title")}
        subtitle={t("dashboard.perimeter.subtitle")}
        actions={
          canOpenZones && (
            <Button variant="secondary" size="sm" icon={ArrowUpRight} onClick={() => openZones()}>
              {t("nav.zones")}
            </Button>
          )
        }
      >
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_300px]">
          <div>
            <PerimeterMap compact selected={null} interactive={canOpenZones} onSelect={openZones} />
            {canOpenZones && <p className="text-center text-xs text-text-muted">{t("dashboard.perimeter.selectHint")}</p>}
            <div className="mt-3 flex flex-wrap justify-center gap-2">
              {LEVEL_ORDER.map((level) => (
                <LevelBadge key={level} level={level} />
              ))}
            </div>
          </div>
          <div>
            <h3 className="text-sm font-semibold text-text">{t("dashboard.gates.title")}</h3>
            {isLoading && <Skeleton className="mt-3 h-32 w-full" />}
            {!isLoading && !error && gates.length === 0 && <EmptyState message={t("dashboard.gates.empty")} />}
            <ul className="divide-y divide-border-subtle">
              {gates.map((gate) => (
                <li key={gate.id} className="flex items-center gap-3 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-text">{gate.name}</p>
                    <p className="truncate text-xs text-text-muted">{gate.detail}</p>
                  </div>
                  <StatusLabel status={gate.status} />
                </li>
              ))}
            </ul>
            <p className="mt-3 rounded-tile bg-surface-muted px-4 py-3 text-xs text-text-muted">{t("dashboard.perimeter.noSensors")}</p>
          </div>
        </div>
      </Card>

      {canViewLog && <RecentActivity onOpenLog={openLog} />}
    </div>
  );
}

export default function Dashboard() {
  return (
    <ScreenAccess screen="dashboard">
      <DashboardContent />
    </ScreenAccess>
  );
}
