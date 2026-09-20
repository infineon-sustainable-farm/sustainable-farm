import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import LowStockNotifications from "./LowStockNotifications";

function MachineryLayout() {
    return (
        <div className="flex h-dvh">
            <Sidebar />
            <main className="flex-1 min-w-0 h-dvh overflow-y-auto">
                <div className="sticky top-0 z-30 h-14 bg-white border-b border-gray-200 flex items-center justify-end px-6">
                    <LowStockNotifications />
                </div>
                <Outlet />
            </main>
        </div>
    );
}
export default MachineryLayout;
