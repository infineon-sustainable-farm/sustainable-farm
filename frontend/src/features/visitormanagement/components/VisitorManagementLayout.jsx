import { Outlet, useLocation } from "react-router-dom";
import VisitorManagementSidebar from "./VisitorManagementSidebar";
import ModuleTopBar from "./ModuleTopBar";

/**
 * Shell of the Visitor Management module, following the approved mockup: the
 * fixed sidebar on the left, and a right column made of the sticky top bar and
 * the active screen below it.
 *
 * The active screen is wrapped in a keyed container so each navigation remounts
 * it and its entrance animation plays again — the smooth, quiet fade-and-rise
 * that gives the module a sense of continuity when moving between screens.
 */
export default function VisitorManagementLayout() {
    const location = useLocation();
    return (
        <div className="flex h-dvh overflow-hidden">
            <VisitorManagementSidebar />
            <div className="ml-0 flex h-dvh min-w-0 flex-1 flex-col overflow-y-auto md:ml-[250px]">
                <ModuleTopBar />
                <main className="min-w-0 flex-1">
                    <div
                        key={location.pathname}
                        className="animate-page-in min-h-full"
                    >
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    );
}