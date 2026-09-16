import { AnimatePresence, motion } from "framer-motion";
import { ArrowUpRight, CheckCircle2, DoorOpen, Layers3, Lock, ShieldAlert, TriangleAlert } from "lucide-react";
import PerimeterMap from "../components/PerimeterMap";
import { Card, Eyebrow, LevelChip, ScreenHeader, SensitivityMeter, StatusChip } from "../components/ui";
import { LEVEL_META, type LevelKey } from "../api/sitesecurityTypes";
import { useZones } from "../hooks/useZones";

const ZONE_ICON: Record<LevelKey, typeof DoorOpen> = {
  open: DoorOpen,
  controlled: Layers3,
  restricted: Lock,
  critical: ShieldAlert,
};

export default function ZonesScreen({
  selected,
  onSelect,
  onOpenLog,
}: {
  selected: LevelKey;
  onSelect: (z: LevelKey) => void;
  onOpenLog: () => void;
}) {
  const { selectedZone: zone } = useZones(selected);

  if (!zone) {
    return (
      <div className="space-y-5">
        <ScreenHeader
          kicker="03 · Site Model"
          title="Perimeter & Zones"
          subtitle="How the farm is divided — and who may cross each line"
        />
        <Card className="flex min-h-64 items-center justify-center">
          <p className="font-sans text-[13px] text-slate-400">Loading zones...</p>
        </Card>
      </div>
    );
  }

  const meta = LEVEL_META[zone.id];
  const Icon = ZONE_ICON[zone.id];

  return (
    <div className="space-y-5">
      <ScreenHeader
        kicker="03 · Site Model"
        title="Perimeter & Zones"
        subtitle="How the farm is divided — and who may cross each line"
        actions={
          <span className="inline-flex items-center gap-2 rounded-full border border-dashed border-amber-300 bg-amber-50 px-3.5 py-2 font-mono text-[10px] font-semibold uppercase tracking-[0.14em] text-amber-700">
            <TriangleAlert className="size-3.5" />
            Preliminary concept · v0.3
          </span>
        }
      />

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        {/* diagram */}
        <div>
          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-6 py-4">
              <div>
                <h2 className="font-display text-[17px] font-semibold text-navy-900">Site Diagram</h2>
                <p className="mt-0.5 text-[12px] text-slate-400 font-sans">
                  Conceptual ring model — deeper rings require stronger verification
                </p>
              </div>
              <div className="flex items-center gap-3">
                {(["open", "controlled", "restricted", "critical"] as LevelKey[]).map((l) => (
                  <span key={l} className="flex items-center gap-1.5 font-sans text-[12px] font-medium text-slate-500">
                    <span className="size-2 rounded-sm" style={{ backgroundColor: LEVEL_META[l].solid }} />
                    {LEVEL_META[l].label}
                  </span>
                ))}
              </div>
            </div>

            <div className="bg-dotgrid p-3">
              <PerimeterMap selected={selected} onSelect={onSelect} />
            </div>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-slate-100 px-6 py-3">
              <span className="flex items-center gap-2 font-sans text-[12px] text-slate-400">
                <span className="h-1 w-8 rounded-full bg-navy-900 [mask-image:radial-gradient(circle,black_50%,transparent_51%)] [mask-size:4px_4px]" />
                Fence line
              </span>
              <span className="flex items-center gap-2 font-sans text-[12px] text-slate-400">
                <span className="inline-block size-3 rounded-[4px] border border-navy-900 bg-white" />
                Access point
              </span>
              <span className="flex items-center gap-2 font-sans text-[12px] text-slate-400">
                <span className="inline-block size-3 rounded-[4px] [background:repeating-linear-gradient(45deg,#ef6c00_0_2px,transparent_2px_5px)] opacity-60" />
                Heightened control
              </span>
            </div>
          </Card>

          {/* validation strip */}
          <div className="mt-3 flex items-center gap-3 rounded-2xl border border-dashed border-amber-300 bg-amber-50/80 px-5 py-3.5">
            <TriangleAlert className="size-4 shrink-0 text-amber-600" />
            <p className="font-sans text-[12px] font-semibold uppercase tracking-wider text-amber-800">
              Preliminary concept — to be validated on site
            </p>
            <p className="ml-auto hidden text-[12px] text-amber-700/80 lg:block font-sans">
              Layout, zone geometry and gate positions require physical survey before implementation.
            </p>
          </div>
        </div>

        {/* selected zone panel */}
        <Card className="h-fit overflow-hidden xl:sticky xl:top-24">
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
            <Eyebrow>Selected zone</Eyebrow>
            <span className="size-2 rounded-full" style={{ backgroundColor: meta.solid }} />
          </div>
          <AnimatePresence mode="wait">
            <motion.div
              key={zone.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.22 }}
              className="px-6 py-5"
            >
              <div className="flex items-start gap-3.5">
                <span
                  className="flex size-11 items-center justify-center rounded-xl border"
                  style={{ backgroundColor: meta.fill, borderColor: `${meta.solid}35`, color: meta.solid }}
                >
                  <Icon className="size-5" strokeWidth={2.1} />
                </span>
                <div>
                  <h3 className="font-display text-[20px] font-semibold leading-tight text-navy-900">
                    {zone.name}
                  </h3>
                  <p className="mt-0.5 text-[12px] text-slate-400 font-sans">{zone.tagline}</p>
                </div>
              </div>

              <div className="mt-4 flex items-center gap-2.5">
                <LevelChip level={zone.id} />
                <SensitivityMeter level={zone.id} />
              </div>

              <p className="mt-4 text-[13px] leading-relaxed text-slate-500 font-sans">{zone.description}</p>

              <div className="mt-5 grid grid-cols-2 gap-3">
                <div className="rounded-xl bg-slate-50 px-4 py-3">
                  <p className="font-display text-[28px] font-bold text-navy-900">{zone.authorizedUsers}</p>
                  <p className="mt-0.5 font-sans text-[12px] font-medium uppercase tracking-wider text-slate-400">
                    Authorized users
                  </p>
                </div>
                <div className="rounded-xl bg-slate-50 px-4 py-3">
                  <p className="font-display text-[28px] font-bold text-navy-900">{zone.entryPoints}</p>
                  <p className="mt-0.5 font-sans text-[12px] font-medium uppercase tracking-wider text-slate-400">
                    Entry points
                  </p>
                </div>
              </div>

              <div className="mt-3 flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3">
                <span className="font-sans text-[12px] font-medium uppercase tracking-wider text-slate-400">Status</span>
                <StatusChip status={zone.status} size="sm" />
              </div>

              <div className="mt-5 border-t border-slate-100 pt-4">
                <Eyebrow className="mb-2.5">Access requirements</Eyebrow>
                <ul className="space-y-2.5">
                  {zone.requirements.map((r) => (
                    <li key={r} className="flex items-start gap-2.5 text-[13px] text-slate-600 font-sans">
                      <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-[#4caf50]" strokeWidth={2.2} />
                      {r}
                    </li>
                  ))}
                </ul>
              </div>

              <button
                onClick={onOpenLog}
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-[13px] font-semibold text-white transition-colors hover:bg-primary-700 font-sans shadow-sm"
              >
                View zone activity
                <ArrowUpRight className="size-4" />
              </button>
            </motion.div>
          </AnimatePresence>
        </Card>
      </div>
    </div>
  );
}
