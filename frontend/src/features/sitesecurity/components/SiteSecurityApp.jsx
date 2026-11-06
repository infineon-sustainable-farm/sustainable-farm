import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import AccessGate from "./AccessGate";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import Dashboard from "../screens/Dashboard";
import AccessControl from "../screens/AccessControl";
import ZonesScreen from "../screens/ZonesScreen";
import AccessLogScreen from "../screens/AccessLogScreen";
import { endSession, getSessionRole, setSessionEndedHandler } from "../api/sitesecurityAuth";
export default function App() {
    const [role, setRole] = useState(getSessionRole);
    const [screen, setScreen] = useState("dashboard");
    const [selectedZone, setSelectedZone] = useState("restricted");
    const [logFilter, setLogFilter] = useState("all");
    const [logZone, setLogZone] = useState(null);
    const [logSeed, setLogSeed] = useState(0);
    useEffect(() => {
        setSessionEndedHandler(() => setRole(null));
        return () => setSessionEndedHandler(null);
    }, []);
    const openZones = (zone) => {
        if (zone)
            setSelectedZone(zone);
        setScreen("zones");
    };
    const openLog = (filter = "all", zone = null) => {
        setLogFilter(filter);
        setLogZone(zone);
        setLogSeed((n) => n + 1);
        setScreen("log");
    };
    const navigate = (next) => {
        if (next === "log") {
            openLog();
        }
        else {
            setScreen(next);
        }
    };
    if (!role) {
        return <AccessGate onAuthenticated={setRole}/>;
    }
    return (<div className="min-h-screen">
      <Sidebar screen={screen} onNavigate={navigate} role={role}/>
      <div className="pl-[240px]">
        <TopBar screen={screen} onLock={endSession}/>
        <main className="mx-auto max-w-[1360px] px-8 pb-14 pt-7">
            <motion.div key={screen} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}>
              {screen === "dashboard" && <Dashboard onOpenZones={openZones} onOpenLog={openLog}/>}
              {screen === "access" && <AccessControl canManage={role === "admin"}/>}
              {screen === "zones" && (<ZonesScreen selected={selectedZone} onSelect={setSelectedZone} onOpenLog={(zone) => openLog("all", zone)}/>)}
              {screen === "log" && <AccessLogScreen key={logSeed} initialFilter={logFilter} initialZone={logZone}/>}
            </motion.div>
        </main>
      </div>
    </div>);
}
