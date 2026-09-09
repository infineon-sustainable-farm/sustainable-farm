import { AnimatePresence, motion } from "framer-motion";
import { AlertTriangle, ArrowRight, CheckCircle2, Clock, X } from "lucide-react";
import { cn } from "../utils/cn";
import { Avatar, Eyebrow, LevelChip, StatusChip } from "./ui";
import type { LogEntry } from "../api/sitesecurityTypes";

const TONE: Record<LogEntry["status"], { bar: string; icon: typeof CheckCircle2; title: string; sub: string }> = {
  Approved: {
    bar: "bg-emerald-50 border-emerald-200",
    icon: CheckCircle2,
    title: "Access approved",
    sub: "Credential matched the zone policy",
  },
  Denied: {
    bar: "bg-rose-50 border-rose-200",
    icon: AlertTriangle,
    title: "Access denied",
    sub: "Recorded and escalated for review",
  },
  Pending: {
    bar: "bg-amber-50 border-amber-200",
    icon: Clock,
    title: "Awaiting approval",
    sub: "Held at gate — supervisor decision required",
  },
};

const ICON_TONE: Record<LogEntry["status"], string> = {
  Approved: "text-emerald-600",
  Denied: "text-rose-600",
  Pending: "text-amber-600",
};

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-6 py-3">
      <span className="pt-0.5 font-mono text-[10px] uppercase tracking-[0.16em] text-slate-400">{label}</span>
      <span className="text-right text-[13px] font-medium text-navy-900">{children}</span>
    </div>
  );
}

export default function LogDrawer({
  entry,
  onClose,
}: {
  entry: LogEntry | null;
  onClose: () => void;
}) {
  return (
    <AnimatePresence>
      {entry && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-navy-950/30 backdrop-blur-[2px]"
            onClick={onClose}
          />
          <motion.aside
            initial={{ x: "104%" }}
            animate={{ x: 0 }}
            exit={{ x: "104%" }}
            transition={{ type: "spring", stiffness: 340, damping: 34 }}
            className="fixed inset-y-0 right-0 z-50 flex w-full max-w-[420px] flex-col bg-white shadow-[0_24px_80px_-20px_rgba(10,17,31,0.45)]"
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <div>
                <Eyebrow>Access event</Eyebrow>
                <p className="mt-0.5 font-mono text-[12px] font-semibold text-navy-900">{entry.ref}</p>
              </div>
              <button
                onClick={onClose}
                className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-navy-900"
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-6 py-5">
              <div className={cn("flex items-center gap-3 rounded-xl border px-4 py-3.5", TONE[entry.status].bar)}>
                {(() => {
                  const Icon = TONE[entry.status].icon;
                  return <Icon className={cn("size-5", ICON_TONE[entry.status])} strokeWidth={2.2} />;
                })()}
                <div>
                  <p className="text-[13.5px] font-semibold text-navy-900">{TONE[entry.status].title}</p>
                  <p className="text-[11.5px] text-slate-500">{TONE[entry.status].sub}</p>
                </div>
                <span className="ml-auto">
                  <StatusChip status={entry.status} size="sm" />
                </span>
              </div>

              <div className="mt-4 flex items-center gap-3.5 rounded-xl border border-slate-100 bg-slate-50/60 px-4 py-3.5">
                <Avatar initials={entry.initials} level={entry.type === "Unknown" ? undefined : entry.zone} />
                <div className="min-w-0">
                  <p className="truncate text-[14px] font-semibold text-navy-900">{entry.user}</p>
                  <p className="text-[11.5px] text-slate-500">{entry.type}</p>
                </div>
                {entry.type === "Unknown" && (
                  <span className="ml-auto rounded-full bg-rose-50 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide text-rose-600">
                    Unidentified
                  </span>
                )}
              </div>

              <div className="mt-2 divide-y divide-slate-100">
                <Row label="Date">{entry.date}</Row>
                <Row label="Time">
                  <span className="font-mono">{entry.time}</span>
                </Row>
                <Row label="Zone">
                  <span className="flex items-center justify-end gap-2">
                    {entry.zoneName}
                    <LevelChip level={entry.zone} size="sm" />
                  </span>
                </Row>
                <Row label="Action">
                  <span className="inline-flex items-center gap-1.5">
                    {entry.action}
                    <ArrowRight className="size-3.5 text-slate-400" />
                    <span className="text-slate-500">{entry.gate}</span>
                  </span>
                </Row>
                <Row label="Verification">
                  <span className="max-w-[220px] leading-snug">{entry.method}</span>
                </Row>
              </div>

              <div className="mt-4 rounded-xl border border-slate-100 px-4 py-3.5">
                <Eyebrow className="mb-1.5">Note</Eyebrow>
                <p className="text-[12.5px] leading-relaxed text-slate-600">{entry.note}</p>
              </div>
            </div>

            <div className="flex items-center gap-2.5 border-t border-slate-100 px-6 py-4">
              <button
                onClick={onClose}
                className="flex-1 rounded-xl bg-navy-900 px-4 py-2.5 text-[13px] font-semibold text-white transition-colors hover:bg-navy-800"
              >
                Mark as reviewed
              </button>
              <button
                onClick={onClose}
                className="rounded-xl border border-slate-200 px-4 py-2.5 text-[13px] font-semibold text-slate-600 transition-colors hover:bg-slate-50"
              >
                Close
              </button>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
