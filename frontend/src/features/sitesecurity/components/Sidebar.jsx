import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Activity, LayoutGrid, KeyRound, Map, ScrollText, } from "lucide-react";
import { cn } from "../utils/cn";
import { Eyebrow } from "./ui";
import { SCREEN_LABEL } from "../api/sitesecurityTypes";
const NAV = [
    { id: "dashboard", icon: LayoutGrid },
    { id: "access", icon: KeyRound },
    { id: "zones", icon: Map },
    { id: "log", icon: ScrollText },
];
export default function Sidebar({ screen, onNavigate, role, }) {
    const [showHealth, setShowHealth] = useState(false);
    const closeHealthOnEscape = (event) => {
        if (event.key === "Escape")
            setShowHealth(false);
    };
    return (<aside className="fixed inset-y-0 left-0 z-40 flex w-[240px] flex-col bg-[#0a8276] text-white shadow-lg">
        {/* brand / logo section */}
        <div className="border-b border-white/15 px-4 pb-5 pt-6 text-center">
          <div className="mx-auto flex size-24 items-center justify-center overflow-hidden rounded-full bg-white p-1 shadow-md">
            <img src="/logo.png" alt="Logo" className="h-full w-full object-contain scale-[1.55]"/>
          </div>
          <div className="mt-3.5">
            <p className="font-display text-[13px] font-bold uppercase tracking-widest text-white">
              FENCELINE SECURITY
            </p>
            <p className="mt-0.5 font-display text-[10px] font-semibold uppercase tracking-wider text-white">
              SECURITY MANAGEMENT
            </p>
          </div>
        </div>

        {/* primary nav */}
        <nav aria-label="Site Security" className="flex-1 space-y-1.5 px-3 pt-5">
          <Eyebrow className="px-2 pb-2 text-white">Operations</Eyebrow>
          {NAV.map((item) => {
            const active = screen === item.id;
            const Icon = item.icon;
            return (<button key={item.id} type="button" aria-current={active ? "page" : undefined} onClick={() => onNavigate(item.id)} className={cn("group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-normal transition-all duration-200", active
                    ? "bg-black/25 text-white font-medium shadow-sm backdrop-blur-sm"
                    : "text-white hover:bg-black/15")}>
                <Icon className={"size-[18px] text-white"} strokeWidth={2}/>
                <span className="font-sans">{SCREEN_LABEL[item.id]}</span>
                {active && (<span className="ml-auto size-1.5 rounded-full bg-[#ef6c00]"/>)}
              </button>);
        })}
        </nav>

        {/* secondary footer nav */}
        <div className="px-3 pb-3 pt-3">
          <div className="mx-2 mb-3 border-t border-white/15"/>
          <div className="relative" onKeyDown={closeHealthOnEscape}>
            <button type="button" aria-expanded={showHealth} aria-controls="site-health" onClick={() => setShowHealth((v) => !v)} className={cn("flex w-full items-center gap-2.5 whitespace-nowrap rounded-xl px-3 py-2.5 text-[14px] font-normal transition-colors", showHealth ? "bg-black/25 text-white" : "text-white hover:bg-black/15")}>
              <Activity className="size-[18px] shrink-0 text-white" strokeWidth={2}/>
              <span className="font-sans">System Status</span>
              <span className="ml-auto font-mono text-[9px] font-medium uppercase text-white">
                No sensors
              </span>
            </button>

            <AnimatePresence>
              {showHealth && (<motion.div id="site-health" initial={{ opacity: 0, y: 10, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 10, scale: 0.97 }} transition={{ duration: 0.18 }} className="absolute bottom-full left-0 z-50 mb-2 w-[220px] rounded-2xl border border-slate-200 bg-white p-4 text-navy-900 shadow-[0_24px_60px_-20px_rgba(10,17,31,0.35)]">
                  <Eyebrow className="mb-2 text-slate-600">Site health</Eyebrow>
                  <p className="text-[12px] leading-relaxed text-slate-600">
                    No fence sensors or gate controllers are connected, so their state is unknown.
                  </p>
                </motion.div>)}
            </AnimatePresence>
          </div>

          <div className="mt-4 flex items-center gap-3 rounded-xl bg-black/20 px-3 py-2.5">
            <span className="min-w-0">
              <span className="block truncate text-[13px] font-medium text-white">{role === "admin" ? "Admin access" : "Read-only access"}</span>
              <span className="block truncate text-[10.5px] text-white">Willow Ridge Farm</span>
            </span>
          </div>
          <p className="pb-2 pt-3 text-center font-mono text-[9px] uppercase tracking-[0.18em] text-white">
            v0.1.0 · demonstration prototype
          </p>
        </div>
      </aside>);
}
