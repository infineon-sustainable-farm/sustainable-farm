import { useEffect, useState, type ReactNode } from "react";
import { ChevronRight, DoorOpen, IdCard, Lock, ShieldAlert } from "lucide-react";
import { cn } from "../utils/cn";
import { LEVEL_META, LEVEL_ORDER, type LevelKey } from "../api/sitesecurityTypes";

export type ScreenId = "dashboard" | "access" | "zones" | "log";

export const SCREEN_LABEL: Record<ScreenId, string> = {
  dashboard: "Dashboard",
  access: "Access Control",
  zones: "Perimeter & Zones",
  log: "Access Log",
};

/* ------------------------------ primitives ----------------------------- */

export function Card({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-slate-200/80 bg-white shadow-[0_1px_2px_rgba(16,27,46,0.04),0_10px_30px_-14px_rgba(16,27,46,0.12)]",
        className
      )}
    >
      {children}
    </div>
  );
}

export function Eyebrow({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <p
      className={cn(
        "font-sans text-[12px] font-medium uppercase tracking-wider text-slate-400",
        className
      )}
    >
      {children}
    </p>
  );
}

export function ScreenHeader({
  kicker,
  title,
  subtitle,
  actions,
}: {
  kicker: string;
  title: string;
  subtitle: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        <Eyebrow className="mb-1.5 text-primary-600 font-semibold">{kicker}</Eyebrow>
        <h1 className="font-display text-[24px] font-semibold leading-tight tracking-tight text-navy-900">
          {title}
        </h1>
        <p className="mt-1 text-[14px] text-slate-500 font-sans">{subtitle}</p>
      </div>
      {actions && <div className="flex items-center gap-3">{actions}</div>}
    </div>
  );
}

export function LevelChip({ level, size = "md" }: { level: LevelKey; size?: "sm" | "md" }) {
  const meta = LEVEL_META[level];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border font-sans font-medium",
        meta.chip,
        size === "sm" ? "px-2 py-0.5 text-[12px]" : "px-2.5 py-1 text-[12px]"
      )}
    >
      <span className={cn("size-1.5 rounded-full", meta.dot)} />
      {meta.label}
    </span>
  );
}

const STATUS_TONE: Record<string, string> = {
  Approved: "bg-emerald-50 text-emerald-700 border-emerald-200",
  Active: "bg-emerald-50 text-emerald-700 border-emerald-200",
  Online: "bg-emerald-50 text-emerald-700 border-emerald-200",
  Secure: "bg-emerald-50 text-emerald-700 border-emerald-200",
  "Open Access": "bg-emerald-50 text-emerald-700 border-emerald-200",
  Authorized: "bg-blue-50 text-blue-700 border-blue-200",
  Monitored: "bg-blue-50 text-blue-700 border-blue-200",
  Protected: "bg-blue-50 text-blue-700 border-blue-200",
  Pending: "bg-amber-50 text-amber-700 border-amber-200",
  Locked: "bg-amber-50 text-amber-700 border-amber-200",
  "Locked Down": "bg-amber-50 text-amber-700 border-amber-200",
  Denied: "bg-rose-50 text-rose-700 border-rose-200",
  Offline: "bg-rose-50 text-rose-700 border-rose-200",
  Expired: "bg-slate-100 text-slate-500 border-slate-200",
  Unknown: "bg-slate-100 text-slate-500 border-slate-200",
};

const STATUS_DOT: Record<string, string> = {
  Approved: "bg-emerald-500",
  Active: "bg-emerald-500",
  Online: "bg-emerald-500",
  Secure: "bg-emerald-500",
  "Open Access": "bg-emerald-500",
  Authorized: "bg-blue-500",
  Monitored: "bg-blue-500",
  Protected: "bg-blue-500",
  Pending: "bg-amber-500",
  Locked: "bg-amber-500",
  "Locked Down": "bg-amber-500",
  Denied: "bg-rose-500",
  Offline: "bg-rose-500",
  Expired: "bg-slate-400",
  Unknown: "bg-slate-400",
};

export function StatusChip({ status, size = "md" }: { status: string; size?: "sm" | "md" }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border font-sans font-medium",
        STATUS_TONE[status] ?? "bg-slate-100 text-slate-600 border-slate-200",
        size === "sm" ? "px-2.5 py-0.5 text-[12px]" : "px-3 py-1 text-[12px]"
      )}
    >
      <span
        className={cn(
          "size-1.5 rounded-full",
          STATUS_DOT[status] ?? "bg-slate-400",
          (status === "Pending" || status === "Denied") && "status-pulse"
        )}
      />
      {status}
    </span>
  );
}

export function SensitivityMeter({ level }: { level: LevelKey }) {
  const meta = LEVEL_META[level];
  return (
    <span className="inline-flex items-center gap-[3px]" title={`Verification strength ${meta.meter}/4`}>
      {LEVEL_ORDER.map((_, i) => (
        <span
          key={i}
          className="h-[8px] w-[7px] rounded-[2px]"
          style={{
            backgroundColor: i < meta.meter ? meta.solid : "#E5E9F0",
            opacity: i < meta.meter ? 0.55 + 0.45 * ((i + 1) / 4) : 1,
          }}
        />
      ))}
    </span>
  );
}

export function Avatar({ initials, level }: { initials: string; level?: LevelKey }) {
  const tone = level ? LEVEL_META[level].avatar : "bg-slate-100 text-slate-500";
  return (
    <span
      className={cn(
        "flex size-9 shrink-0 items-center justify-center rounded-full text-[12px] font-semibold tracking-wide font-sans",
        tone
      )}
    >
      {initials}
    </span>
  );
}

export function FilterPill({
  active,
  onClick,
  children,
  count,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
  count?: number;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-[13px] font-medium font-sans transition-all duration-200",
        active
          ? "border-primary-600 bg-primary-600 text-white shadow-sm"
          : "border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-navy-900"
      )}
    >
      {children}
      {typeof count === "number" && (
        <span
          className={cn(
            "rounded-full px-1.5 py-px font-mono text-[11px]",
            active ? "bg-white/20 text-white" : "bg-slate-100 text-slate-500"
          )}
        >
          {count}
        </span>
      )}
    </button>
  );
}

export function LiveClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);
  return (
    <span className="font-mono text-[11.5px] tabular-nums text-slate-500">
      {now.toLocaleDateString("en-GB", { weekday: "short", day: "2-digit", month: "short", year: "numeric" })}
      <span className="mx-1.5 text-slate-300">·</span>
      {now.toLocaleTimeString("en-GB", { hour12: false })}
    </span>
  );
}

/* ------------------------- sensitivity ladder strip --------------------- */

const LADDER: { level: LevelKey; icon: typeof DoorOpen; caption: string }[] = [
  { level: "open", icon: DoorOpen, caption: "Visitor check-in" },
  { level: "controlled", icon: IdCard, caption: "Staff credential" },
  { level: "restricted", icon: Lock, caption: "Authorised personnel only" },
  { level: "critical", icon: ShieldAlert, caption: "Dual verification + alert" },
];

export function SecurityLadder() {
  return (
    <Card className="flex flex-wrap items-center gap-y-3 px-5 py-4">
      <div className="mr-2 min-w-[150px]">
        <Eyebrow>Access level model</Eyebrow>
        <p className="mt-0.5 text-[12px] text-slate-500">Deeper zones demand stronger proof</p>
      </div>
      <div className="flex flex-1 flex-wrap items-center gap-x-1 gap-y-3">
        {LADDER.map((step, i) => {
          const meta = LEVEL_META[step.level];
          const Icon = step.icon;
          return (
            <div key={step.level} className="flex items-center">
              {i > 0 && <ChevronRight className="mx-1.5 size-4 text-slate-300" strokeWidth={2.5} />}
              <div className="flex items-center gap-2.5">
                <span
                  className="flex size-9 items-center justify-center rounded-xl border"
                  style={{ backgroundColor: meta.fill, borderColor: `${meta.solid}33`, color: meta.solid }}
                >
                  <Icon className="size-4" strokeWidth={2.2} />
                </span>
                <span>
                  <span className="flex items-center gap-2 text-[12.5px] font-semibold text-navy-900">
                    {meta.label}
                    <SensitivityMeter level={step.level} />
                  </span>
                  <span className="block text-[11px] text-slate-400">{step.caption}</span>
                </span>
              </div>
            </div>
          );
        })}
      </div>
      <span className="ml-auto hidden font-mono text-[10px] uppercase tracking-[0.18em] text-slate-400 lg:block">
        Verification strength →
      </span>
    </Card>
  );
}
