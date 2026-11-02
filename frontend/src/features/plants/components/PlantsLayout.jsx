import { Outlet } from "react-router-dom";
import PlantsSidebar from "./PlantsSidebar";

/**
 * Shell of the Plants module: its own sidebar, with the module's screens
 * rendered beside it. Same structure as MachineryLayout, so both modules carry
 * their navigation the same way.
 */
export default function PlantsLayout() {
    return (
        <div className="flex h-dvh">
            <PlantsSidebar />
            {/* tabular-nums is inherited: every figure of the module, chart labels included, lines up in columns. */}
            <main className="h-dvh min-w-0 flex-1 overflow-y-auto tabular-nums">
                <Outlet />
            </main>
        </div>
    );
}
