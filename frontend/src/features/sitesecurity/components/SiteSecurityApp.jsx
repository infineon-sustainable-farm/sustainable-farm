import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import { Card } from "./ui";
import Dashboard from "../screens/Dashboard";
import AccessControl from "../screens/AccessControl";
import ZonesScreen from "../screens/ZonesScreen";
import AccessLogScreen from "../screens/AccessLogScreen";
import { restoreSession } from "../api/sitesecurityApi";
import { isAdmin, visibleScreens } from "../utils/accessRights";
export default function App() {
    const [session, setSession] = useState(null);
    const [loading, setLoading] = useState(true);
    const [screen, setScreen] = useState("dashboard");
    const [selectedZone, setSelectedZone] = useState("restricted");
    const [logFilter, setLogFilter] = useState("all");
    const [logZone, setLogZone] = useState(null);
    const [logSeed, setLogSeed] = useState(0);
    useEffect(() => {
        let active = true;
        restoreSession().then((identity) => active && setSession(identity)).catch(() => active && setSession(null)).finally(() => active && setLoading(false));
        return () => {
            active = false;
        };
    }, []);
    if (loading) {
        return <main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6"><p role="status">Loading Site Security identity…</p></main>;
    }
    if (!session) {
        return <main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6"><p role="alert">Central platform identity is required.</p></main>;
    }
    const screens = visibleScreens(session);
    /* A screen the account may not open falls back to the first one it may. */
    const current = screens.includes(screen) ? screen : screens[0];
    const openZones = (zone) => {
        if (!screens.includes("zones"))
            return;
        if (zone)
            setSelectedZone(zone);
        setScreen("zones");
    };
    const openLog = (filter = "all", zone = null) => {
        if (!screens.includes("log"))
            return;
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
    return (<div className="min-h-screen">
      <Sidebar screen={current} onNavigate={navigate} session={session} screens={screens}/>
      <div className="pl-[240px]">
        <TopBar screen={current}/>
        <main className="mx-auto max-w-[1360px] px-8 pb-14 pt-7">
            {!current && (<Card className="p-6">
                <h1 className="font-display text-[18px] font-semibold text-navy-900">No screens assigned</h1>
                <p className="mt-1 font-sans text-[13px] text-slate-600">Your account has no view permission yet. Ask an administrator to grant one.</p>
              </Card>)}
            {current && (<motion.div key={current} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}>
              {current === "dashboard" && (<Dashboard onOpenZones={openZones} onOpenLog={openLog} canOpenZones={screens.includes("zones")} canViewLog={screens.includes("log")}/>)}
              {current === "access" && <AccessControl canManage={isAdmin(session)}/>}
              {current === "zones" && (<ZonesScreen selected={selectedZone} onSelect={setSelectedZone} onOpenLog={(zone) => openLog("all", zone)}/>)}
              {current === "log" && <AccessLogScreen key={logSeed} initialFilter={logFilter} initialZone={logZone}/>}
            </motion.div>)}
        </main>
      </div>
    </div>);
}
