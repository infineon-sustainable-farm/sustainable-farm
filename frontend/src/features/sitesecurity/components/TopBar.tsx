import { Bell, ChevronRight, MapPin } from "lucide-react";
import { LiveClock, SCREEN_LABEL, type ScreenId } from "./ui";

export default function TopBar({ screen }: { screen: ScreenId }) {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200/70 bg-[#f4f6f9]/85 px-8 backdrop-blur-md">
      <div className="flex items-center gap-2 font-mono text-[10.5px] uppercase tracking-[0.2em] text-slate-400">
        <span className="text-navy-900">Fenceline</span>
        <ChevronRight className="size-3" />
        <span>{SCREEN_LABEL[screen]}</span>
      </div>
      <div className="flex items-center gap-5">
        <span className="hidden items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-[11.5px] font-medium text-slate-500 xl:flex">
          <MapPin className="size-3.5 text-accent-500" />
          Willow Ridge Farm
        </span>
        <LiveClock />
        <button className="relative rounded-full border border-slate-200 bg-white p-2 text-slate-400 transition-colors hover:text-navy-900">
          <Bell className="size-4" />
          <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-accent-500 ring-2 ring-white" />
        </button>
        <span className="flex size-8 items-center justify-center rounded-full bg-navy-900 text-[10px] font-semibold text-white">
          FM
        </span>
      </div>
    </header>
  );
}
