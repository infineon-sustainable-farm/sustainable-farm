import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import AccessGate from "./AccessGate";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import SetPasswordCard from "./SetPasswordCard";
import ChangePasswordModal from "./ChangePasswordModal";
import { Card } from "./ui";
import Dashboard from "../screens/Dashboard";
import AccessControl from "../screens/AccessControl";
import ZonesScreen from "../screens/ZonesScreen";
import AccessLogScreen from "../screens/AccessLogScreen";
import UsersScreen from "../screens/UsersScreen";
import { getSession, setSessionEndedHandler } from "../api/sitesecurityAuth";
import { signOut } from "../api/sitesecurityApi";
import { isAdmin, visibleScreens } from "../utils/accessRights";
export default function App() {
    const [session, setSession] = useState(getSession);
    const [screen, setScreen] = useState("dashboard");
    const [selectedZone, setSelectedZone] = useState("restricted");
    const [logFilter, setLogFilter] = useState("all");
    const [logZone, setLogZone] = useState(null);
    const [logSeed, setLogSeed] = useState(0);
    const [changingPassword, setChangingPassword] = useState(false);
    useEffect(() => {
        setSessionEndedHandler(() => setSession(null));
        return () => setSessionEndedHandler(null);
    }, []);
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
    if (!session) {
        return <AccessGate onAuthenticated={setSession}/>;
    }
    return (<div className="min-h-screen">
      <Sidebar screen={current} onNavigate={navigate} session={session} screens={screens}/>
      <div className="pl-[240px]">
        <TopBar screen={current} onLock={() => signOut().catch(() => {})} onChangePassword={session.passwordSet ? () => setChangingPassword(true) : undefined}/>
        <main className="mx-auto max-w-[1360px] px-8 pb-14 pt-7">
            {!session.passwordSet && <SetPasswordCard session={session} onSessionChange={setSession}/>}
            {!current && (<Card className="p-6">
                <h1 className="font-display text-[18px] font-semibold text-navy-900">No screens assigned</h1>
                <p className="mt-1 font-sans text-[13px] text-slate-600">Your account has no view permission yet. Ask an administrator to grant one.</p>
              </Card>)}
            {current && (<motion.div key={current} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}>
              {current === "dashboard" && (<Dashboard onOpenZones={openZones} onOpenLog={openLog} canOpenZones={screens.includes("zones")} canViewLog={screens.includes("log")} isAdmin={isAdmin(session)} onOpenUsers={() => setScreen("users")}/>)}
              {current === "access" && <AccessControl canManage={isAdmin(session)}/>}
              {current === "zones" && (<ZonesScreen selected={selectedZone} onSelect={setSelectedZone} onOpenLog={(zone) => openLog("all", zone)}/>)}
              {current === "log" && <AccessLogScreen key={logSeed} initialFilter={logFilter} initialZone={logZone}/>}
              {current === "users" && <UsersScreen />}
            </motion.div>)}
        </main>
      </div>
      <ChangePasswordModal open={changingPassword} onClose={() => setChangingPassword(false)} email={session.email}/>
    </div>);
}
