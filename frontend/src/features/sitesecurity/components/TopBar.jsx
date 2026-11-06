import { ChevronRight, Lock, MapPin } from "lucide-react";
import { LiveClock } from "./ui";
import { SCREEN_LABEL } from "../api/sitesecurityTypes";
export default function TopBar({ screen, onLock }) {
    return (<header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200/70 bg-[#f4f6f9]/85 px-8 backdrop-blur-md">
      <div className="flex items-center gap-2 font-mono text-[10.5px] uppercase tracking-[0.2em] text-slate-400">
        <span className="text-navy-900">Fenceline</span>
        <ChevronRight className="size-3"/>
        <span>{SCREEN_LABEL[screen]}</span>
      </div>
      <div className="flex items-center gap-5">
        <span className="hidden items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-[11.5px] font-medium text-slate-500 xl:flex">
          <MapPin className="size-3.5 text-accent-500"/>
          Willow Ridge Farm
        </span>
        <LiveClock />
        <button type="button" onClick={onLock} aria-label="Lock session" title="Lock session" className="rounded-full border border-slate-200 bg-white p-2 text-slate-400 transition-colors hover:text-navy-900">
          <Lock className="size-4"/>
        </button>
      </div>
    </header>);
}
