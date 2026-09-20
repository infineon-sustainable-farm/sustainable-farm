import { Outlet } from "react-router-dom";
import VisitorManagementSidebar from "./VisitorManagementSidebar";
import ModuleTopBar from "./ModuleTopBar";

/**
 * Shell of the Visitor Management module, following the approved mockup: the
 * fixed sidebar on the left, and a right column made of the sticky top bar and
 * the active screen below it.
 */
export default function VisitorManagementLayout() {
    return (
        <div className="flex h-dvh overflow-hidden">
            <VisitorManagementSidebar />
            <div className="ml-0 flex h-dvh min-w-0 flex-1 flex-col overflow-y-auto md:ml-[250px]">
                <ModuleTopBar />
                <main className="min-w-0 flex-1">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}