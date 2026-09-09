import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  Activity,
  CheckCircle2,
  LayoutGrid,
  KeyRound,
  Map,
  ScrollText,
  Settings,
  Shield,
  X,
} from "lucide-react";
import { cn } from "../utils/cn";
import { Eyebrow, SCREEN_LABEL, type ScreenId } from "./ui";

const NAV: { id: ScreenId; icon: typeof LayoutGrid }[] = [
  { id: "dashboard", icon: LayoutGrid },
  { id: "access", icon: KeyRound },
  { id: "zones", icon: Map },
  { id: "log", icon: ScrollText },
];

const HEALTH = [
  { label: "Fence loop continuity", value: "OK" },
  { label: "Gate controllers", value: "4 / 4 online" },
  { label: "Perimeter sensors", value: "96 / 96 OK" },
  { label: "Last sync", value: "2 min ago" },
];

export default function Sidebar({
  screen,
  onNavigate,
}: {
  screen: ScreenId;
  onNavigate: (s: ScreenId) => void;
}) {
  const [showHealth, setShowHealth] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [hints, setHints] = useState(true);

  return (
    <>
      <aside className="fixed inset-y-0 left-0 z-40 flex w-[240px] flex-col bg-[#0a8276] text-white shadow-lg">
        {/* brand / logo section */}
        <div className="border-b border-white/15 px-4 pb-5 pt-6 text-center">
          <div className="mx-auto flex size-24 items-center justify-center overflow-hidden rounded-full bg-white p-1 shadow-md">
            <img src="/logo.png" alt="Logo" className="h-full w-full object-contain scale-[1.55]" />
          </div>
          <div className="mt-3.5">
            <p className="font-display text-[13px] font-bold uppercase tracking-widest text-white">
              FENCELINE SECURITY
            </p>
            <p className="mt-0.5 font-display text-[10px] font-semibold uppercase tracking-wider text-white/75">
              SECURITY MANAGEMENT
            </p>
          </div>
        </div>

        {/* primary nav */}
        <nav className="flex-1 space-y-1.5 px-3 pt-5">
          <Eyebrow className="px-2 pb-2 text-white/60">Operations</Eyebrow>
          {NAV.map((item) => {
            const active = screen === item.id;
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={cn(
                  "group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-normal transition-all duration-200",
                  active
                    ? "bg-white/20 text-white font-medium shadow-sm backdrop-blur-sm"
                    : "text-white/80 hover:bg-white/10 hover:text-white"
                )}
              >
                <Icon
                  className={cn("size-[18px]", active ? "text-white" : "text-white/70 group-hover:text-white")}
                  strokeWidth={2}
                />
                <span className="font-sans">{SCREEN_LABEL[item.id]}</span>
                {item.id === "log" && (
                  <span
                    className={cn(
                      "ml-auto rounded-full px-2 py-0.5 font-mono text-[10px] font-semibold",
                      active ? "bg-[#ef6c00] text-white" : "bg-white/20 text-white"
                    )}
                  >
                    1
                  </span>
                )}
                {active && item.id !== "log" && (
                  <span className="ml-auto size-1.5 rounded-full bg-[#ef6c00]" />
                )}
              </button>
            );
          })}
        </nav>

        {/* secondary footer nav */}
        <div className="px-3 pb-3 pt-3">
          <div className="mx-2 mb-3 border-t border-white/15" />
          <div className="relative">
            <button
              onClick={() => setShowHealth((v) => !v)}
              className={cn(
                "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-normal transition-colors",
                showHealth ? "bg-white/20 text-white" : "text-white/80 hover:bg-white/10 hover:text-white"
              )}
            >
              <span className="relative flex size-[18px] items-center justify-center">
                <Activity className="size-[18px] text-[#4caf50]" strokeWidth={2} />
              </span>
              <span className="font-sans">System Status</span>
              <span className="ml-auto flex items-center gap-1.5 font-mono text-[10px] font-medium uppercase tracking-wider text-emerald-200">
                <span className="size-1.5 rounded-full bg-[#4caf50] status-pulse" />
                Nominal
              </span>
            </button>

            <AnimatePresence>
              {showHealth && (
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.97 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.97 }}
                  transition={{ duration: 0.18 }}
                  className="absolute bottom-full left-0 z-50 mb-2 w-[220px] rounded-2xl border border-slate-200 bg-white p-4 text-navy-900 shadow-[0_24px_60px_-20px_rgba(10,17,31,0.35)]"
                >
                  <Eyebrow className="mb-3 text-slate-400">Site health — simulated</Eyebrow>
                  <div className="space-y-2.5">
                    {HEALTH.map((row) => (
                      <div key={row.label} className="flex items-center justify-between text-[12px]">
                        <span className="text-slate-500">{row.label}</span>
                        <span className="inline-flex items-center gap-1.5 font-medium text-navy-900">
                          <CheckCircle2 className="size-3.5 text-[#4caf50]" strokeWidth={2.4} />
                          {row.value}
                        </span>
                      </div>
                    ))}
                  </div>
                  <p className="mt-3 border-t border-slate-100 pt-2.5 font-mono text-[9.5px] uppercase tracking-[0.14em] text-slate-400">
                    No backend connected — mock telemetry
                  </p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button
            onClick={() => setShowSettings(true)}
            className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-normal text-white/80 transition-colors hover:bg-white/10 hover:text-white"
          >
            <Settings className="size-[18px] text-white/70" strokeWidth={2} />
            <span className="font-sans">Settings</span>
          </button>

          <div className="mt-4 flex items-center gap-3 rounded-xl bg-white/10 px-3 py-2.5">
            <span className="flex size-8 items-center justify-center rounded-full bg-white text-[11px] font-bold text-[#0a8276]">
              FM
            </span>
            <span className="min-w-0">
              <span className="block truncate text-[13px] font-medium text-white">Farm Manager</span>
              <span className="block truncate text-[10.5px] text-white/70">Willow Ridge Farm</span>
            </span>
          </div>
          <p className="pb-2 pt-3 text-center font-mono text-[9px] uppercase tracking-[0.18em] text-white/50">
            v0.1.0 · demonstration prototype
          </p>
        </div>
      </aside>

      {/* Settings modal (illustrative only) */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/40 p-6 backdrop-blur-sm"
            onClick={() => setShowSettings(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.96, y: 12 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.96, y: 12 }}
              transition={{ duration: 0.2 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-[420px] rounded-2xl border border-slate-200 bg-white p-6 shadow-[0_24px_60px_-20px_rgba(10,17,31,0.35)]"
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-display text-[17px] font-semibold text-navy-900">Settings</h3>
                  <p className="mt-0.5 text-[12.5px] text-slate-500">
                    Prototype display preferences. Nothing is stored.
                  </p>
                </div>
                <button
                  onClick={() => setShowSettings(false)}
                  className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-navy-900"
                >
                  <X className="size-4" />
                </button>
              </div>
              <div className="mt-5 space-y-4">
                <label className="flex cursor-pointer items-center justify-between text-[13px] font-medium text-navy-900">
                  Show sensitivity hints
                  <button
                    role="switch"
                    aria-checked={hints}
                    onClick={(e) => {
                      e.preventDefault();
                      setHints((v) => !v);
                    }}
                    className={cn(
                      "relative h-6 w-11 rounded-full transition-colors",
                      hints ? "bg-navy-900" : "bg-slate-200"
                    )}
                  >
                    <span
                      className={cn(
                        "absolute top-0.5 size-5 rounded-full bg-white shadow transition-all",
                        hints ? "left-[22px]" : "left-0.5"
                      )}
                    />
                  </button>
                </label>
                <p className="rounded-xl bg-slate-50 px-4 py-3 text-[12px] leading-relaxed text-slate-500">
                  This is a presentation mock-up. Configuration, authentication and real
                  integrations are intentionally out of scope.
                </p>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
