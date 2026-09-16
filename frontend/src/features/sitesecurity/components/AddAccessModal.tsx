import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { HardHat, KeyRound, ShieldCheck, UserRound, Wrench, X } from "lucide-react";
import { cn } from "../utils/cn";
import { Eyebrow, SensitivityMeter } from "./ui";
import { LEVEL_META, LEVEL_ORDER, type LevelKey, type User, type UserType } from "../api/sitesecurityTypes";

const TYPES: { type: UserType; icon: typeof UserRound; hint: string }[] = [
  { type: "Staff", icon: HardHat, hint: "Daily operations" },
  { type: "Visitor", icon: UserRound, hint: "Time-boxed pass" },
  { type: "Service", icon: Wrench, hint: "Contractors & teams" },
  { type: "Security", icon: ShieldCheck, hint: "Full oversight" },
];

function initialsOf(name: string) {
  return (
    name
      .trim()
      .split(/\s+/)
      .map((w) => w[0])
      .slice(0, 2)
      .join("")
      .toUpperCase() || "?"
  );
}

export default function AddAccessModal({
  open,
  onClose,
  onAdd,
}: {
  open: boolean;
  onClose: () => void;
  onAdd: (u: User) => void;
}) {
  const [name, setName] = useState("");
  const [type, setType] = useState<UserType>("Staff");
  const [level, setLevel] = useState<LevelKey>("controlled");
  const [validUntil, setValidUntil] = useState("2026-12-31");

  const reset = () => {
    setName("");
    setType("Staff");
    setLevel("controlled");
    setValidUntil("2026-12-31");
  };

  const submit = () => {
    if (!name.trim()) return;
    const date = new Date(validUntil + "T00:00:00");
    onAdd({
      id: `u-${Date.now()}`,
      name: name.trim(),
      role: "Credential holder — added in prototype",
      type,
      level,
      validUntil: date.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }),
      status: type === "Visitor" ? "Active" : "Authorized",
      initials: initialsOf(name),
      lastActive: "Not yet on site",
    });
    reset();
    onClose();
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/40 p-6 backdrop-blur-sm"
          onClick={onClose}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 14 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 14 }}
            transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-[480px] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_24px_60px_-20px_rgba(10,17,31,0.4)]"
          >
            <div className="flex items-start justify-between px-6 pb-4 pt-6">
              <div>
                <Eyebrow className="mb-1 text-accent-500">New credential</Eyebrow>
                <h3 className="font-display text-[19px] font-semibold text-navy-900">Grant Access</h3>
              </div>
              <button
                onClick={onClose}
                className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-navy-900"
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="space-y-5 px-6 pb-2">
              {/* name */}
              <div>
                <label className="mb-1.5 block text-[12px] font-semibold text-slate-600">Name</label>
                <input
                  autoFocus
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Alex Fourie"
                  className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-[13.5px] text-navy-900 outline-none transition-all placeholder:text-slate-300 focus:border-accent-500 focus:ring-4 focus:ring-accent-100"
                />
              </div>

              {/* user type */}
              <div>
                <label className="mb-1.5 block text-[12px] font-semibold text-slate-600">User type</label>
                <div className="grid grid-cols-4 gap-2">
                  {TYPES.map((t) => {
                    const Icon = t.icon;
                    const active = type === t.type;
                    return (
                      <button
                        key={t.type}
                        onClick={() => setType(t.type)}
                        className={cn(
                          "rounded-xl border px-2 py-2.5 text-center transition-all",
                          active
                            ? "border-navy-900 bg-navy-900 text-white"
                            : "border-slate-200 bg-white text-slate-500 hover:border-slate-300"
                        )}
                      >
                        <Icon className={cn("mx-auto size-4", active ? "text-white" : "text-slate-400")} />
                        <span className="mt-1 block text-[11px] font-semibold">{t.type}</span>
                        <span className={cn("block text-[9px]", active ? "text-white/60" : "text-slate-400")}>
                          {t.hint}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* access level */}
              <div>
                <label className="mb-1.5 flex items-center justify-between text-[12px] font-semibold text-slate-600">
                  Access level
                  <span className="font-mono text-[9.5px] font-normal uppercase tracking-[0.14em] text-slate-400">
                    higher level = stronger control
                  </span>
                </label>
                <div className="grid grid-cols-4 gap-2">
                  {LEVEL_ORDER.map((l) => {
                    const meta = LEVEL_META[l];
                    const active = level === l;
                    return (
                      <button
                        key={l}
                        onClick={() => setLevel(l)}
                        className={cn(
                          "rounded-xl border px-2 py-2.5 text-center transition-all",
                          active ? "border-transparent" : "border-slate-200 bg-white hover:border-slate-300"
                        )}
                        style={active ? { backgroundColor: meta.fill, borderColor: meta.solid } : undefined}
                      >
                        <span
                          className="block text-[11px] font-bold uppercase tracking-wide"
                          style={{ color: active ? meta.solid : "#64748B" }}
                        >
                          {meta.label}
                        </span>
                        <span className="mt-1.5 flex justify-center">
                          <SensitivityMeter level={l} />
                        </span>
                      </button>
                    );
                  })}
                </div>
                <p className="mt-2 text-[11.5px] leading-relaxed text-slate-400">
                  {level === "open" && "Open areas only — no farm credential needed, visitor check-in applies."}
                  {level === "controlled" && "Adds the Controlled Zone — staff credential required at gates."}
                  {level === "restricted" && "Adds the Restricted Zone — authorised personnel, entry is guarded."}
                  {level === "critical" && "Adds the Critical Zone — dual verification and supervisor alert on entry."}
                </p>
              </div>

              {/* valid until */}
              <div>
                <label className="mb-1.5 block text-[12px] font-semibold text-slate-600">Valid until</label>
                <input
                  type="date"
                  value={validUntil}
                  min="2026-05-12"
                  onChange={(e) => setValidUntil(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 font-mono text-[13px] text-navy-900 outline-none transition-all focus:border-accent-500 focus:ring-4 focus:ring-accent-100"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2.5 px-6 py-5">
              <button
                onClick={() => {
                  reset();
                  onClose();
                }}
                className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[13px] font-semibold text-slate-600 transition-colors hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={submit}
                disabled={!name.trim()}
                className="inline-flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2.5 text-[13px] font-semibold text-white shadow-[0_8px_20px_-8px_rgba(178,58,104,0.7)] transition-all hover:bg-accent-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none"
              >
                <KeyRound className="size-4" />
                Grant Access
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
