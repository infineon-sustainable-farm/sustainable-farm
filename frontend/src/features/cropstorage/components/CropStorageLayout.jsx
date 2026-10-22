import { Outlet } from "react-router-dom";
import CropStorageSidebar from "./CropStorageSidebar";

export default function CropStorageLayout() {
    return (
        <div className="flex h-dvh">
            <CropStorageSidebar />
            <main className="h-dvh min-w-0 flex-1 overflow-y-auto">
                <Outlet />
            </main>
        </div>
    );
}
