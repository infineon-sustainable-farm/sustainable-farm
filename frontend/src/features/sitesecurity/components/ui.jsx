import { useTranslation } from "react-i18next";
import { ChevronRight, DoorOpen, IdCard, Lock, ShieldAlert } from "lucide-react";
import { Badge, Card, StatusBadge } from "../../../shared/ui";
import { LEVEL_ORDER, levelMeta } from "../api/sitesecurityTypes";
import { statusTone, translateEnum } from "../utils/status";
import { cn } from "../utils/cn";

/*
 * Site Security visuals with no equivalent in the shared kit (access level badge, verification
 * meter, level ladder), built on the shared components and the design tokens only.
 */

const STATUS_GROUP = "status";

export function LevelBadge({ level, size = "sm" }) {
  const { t } = useTranslation("sitesecurity");
  const meta = levelMeta(level);
  return (
    <Badge tone={meta.tone} size={size}>
      {t(`levels.${meta.id}`)}
    </Badge>
  );
}

export function StatusLabel({ status }) {
  const { t } = useTranslation("sitesecurity");
  const label = translateEnum(t, STATUS_GROUP, status);
  return <StatusBadge tone={statusTone(status)} label={label} />;
}

export function SensitivityMeter({ level }) {
  const { t } = useTranslation("sitesecurity");
  const meta = levelMeta(level);
  const label = t("levels.strength", { value: meta.meter, max: LEVEL_ORDER.length });
  return (
    <span role="img" aria-label={label} title={label} className="inline-flex items-center gap-1">
      {LEVEL_ORDER.map((step, i) => (
        <span
          key={step}
          className="h-2 w-2 rounded-sm"
          style={{ backgroundColor: i < meta.meter ? meta.solid : "var(--color-border-divider)" }}
        />
      ))}
    </span>
  );
}

export function Avatar({ initials, level }) {
  const meta = levelMeta(level);
  return (
    <span
      aria-hidden="true"
      className="flex size-9 shrink-0 items-center justify-center rounded-pill text-xs font-semibold"
      style={{ backgroundColor: meta.fill, color: meta.solid }}
    >
      {initials ?? "?"}
    </span>
  );
}

/* A filter button with its count, as a toggle. */
export function FilterButton({ active, onClick, children, count }) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-2 rounded-pill border px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring",
        active
          ? "border-primary bg-primary text-on-primary"
          : "border-border-input bg-surface-raised text-text-muted hover:text-text"
      )}
    >
      {children}
      {typeof count === "number" && (
        <span className={cn("rounded-pill px-1.5 text-xs", active ? "bg-primary-pressed" : "bg-surface-muted")}>{count}</span>
      )}
    </button>
  );
}

/* An inline message of the screen (error or warning), in the tokens of the kit. */
export function InlineAlert({ tone = "error", children }) {
  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={cn(
        "rounded-tile border px-4 py-3 text-sm",
        tone === "error" ? "border-error-fg bg-error-tint text-error-fg" : "border-warning-fg bg-warning-tint text-warning-fg"
      )}
    >
      {children}
    </div>
  );
}

const LADDER = [
  { level: "open", icon: DoorOpen },
  { level: "controlled", icon: IdCard },
  { level: "restricted", icon: Lock },
  { level: "critical", icon: ShieldAlert },
];

export function SecurityLadder() {
  const { t } = useTranslation("sitesecurity");
  return (
    <Card title={t("ladder.title")} subtitle={t("ladder.subtitle")}>
      <div className="flex flex-wrap items-center gap-x-1 gap-y-3">
        {LADDER.map((step, i) => {
          const meta = levelMeta(step.level);
          const Icon = step.icon;
          return (
            <div key={step.level} className="flex items-center">
              {i > 0 && <ChevronRight aria-hidden="true" className="mx-1.5 size-4 text-text-muted" />}
              <div className="flex items-center gap-2.5">
                <span
                  className="flex size-9 items-center justify-center rounded-tile"
                  style={{ backgroundColor: meta.fill, color: meta.solid }}
                >
                  <Icon aria-hidden="true" className="size-4" />
                </span>
                <span>
                  <span className="flex items-center gap-2 text-sm font-semibold text-text">
                    {t(`levels.${step.level}`)}
                    <SensitivityMeter level={step.level} />
                  </span>
                  <span className="block text-xs text-text-muted">{t(`ladder.${step.level}`)}</span>
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </Card>
  );
}
