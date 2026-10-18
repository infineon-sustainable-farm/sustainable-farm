import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";

function MachineryLayout() {
    return (
        <div className="flex h-dvh">
            <Sidebar />
            <main className="flex-1 min-w-0 h-dvh overflow-y-auto">
                <Outlet />
            </main>
        </div>
    );
}
export default MachineryLayout;
