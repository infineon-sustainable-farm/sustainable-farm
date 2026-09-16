import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import type { ScreenId } from "./ui";
import Dashboard from "../screens/Dashboard";
import AccessControl from "../screens/AccessControl";
import ZonesScreen from "../screens/ZonesScreen";
import AccessLogScreen from "../screens/AccessLogScreen";
import type { LevelKey, LogFilter } from "../api/sitesecurityTypes";

export default function App() {
  const [screen, setScreen] = useState<ScreenId>("dashboard");
  const [selectedZone, setSelectedZone] = useState<LevelKey>("restricted");
  const [logFilter, setLogFilter] = useState<LogFilter>("all");
  const [logSeed, setLogSeed] = useState(0);

  const openZones = (zone?: LevelKey) => {
    if (zone) setSelectedZone(zone);
    setScreen("zones");
  };

  const openLog = (filter?: LogFilter) => {
    if (filter) {
      setLogFilter(filter);
      setLogSeed((n) => n + 1);
    }
    setScreen("log");
  };

  return (
    <div className="min-h-screen">
      <Sidebar screen={screen} onNavigate={setScreen} />
      <div className="pl-[240px]">
        <TopBar screen={screen} />
        <main className="mx-auto max-w-[1360px] px-8 pb-14 pt-7">
          <AnimatePresence mode="wait">
            <motion.div
              key={screen}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
            >
              {screen === "dashboard" && <Dashboard onOpenZones={openZones} onOpenLog={openLog} />}
              {screen === "access" && <AccessControl />}
              {screen === "zones" && (
                <ZonesScreen selected={selectedZone} onSelect={setSelectedZone} onOpenLog={() => openLog("all")} />
              )}
              {screen === "log" && <AccessLogScreen key={`${logFilter}-${logSeed}`} initialFilter={logFilter} />}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
