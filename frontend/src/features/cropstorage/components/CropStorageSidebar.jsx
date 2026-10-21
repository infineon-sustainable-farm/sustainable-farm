import { useState } from "react";
import { Boxes, MapPinned, Menu, Warehouse, X } from "lucide-react";
import { NavLink } from "react-router-dom";

const NAV_ITEMS = [
    { label: "Crop Storage", path: "/cropstorage", icon: Warehouse, end: true },
    { label: "Zones", path: "/cropstorage/zones", icon: MapPinned },
    { label: "Batches", path: "/cropstorage/batches", icon: Boxes },
];

const ITEM_BASE = "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm";

const linkStyles = ({ isActive }) =>
    `${ITEM_BASE} font-medium text-white hover:bg-white/10 ${
        isActive ? "bg-black/20 font-semibold shadow-[inset_3px_0_0_#fff]" : ""
    }`;

export default function CropStorageSidebar() {
    const [isOpen, setIsOpen] = useState(false);

    return (
        <>
            <button
                type="button"
                aria-label={isOpen ? "Close navigation" : "Open navigation"}
                aria-expanded={isOpen}
                onClick={() => setIsOpen(!isOpen)}
                className="fixed top-4 left-4 z-50 rounded-lg bg-primary p-2 text-white md:hidden"
            >
                {isOpen ? <X /> : <Menu />}
            </button>

            <nav
                aria-label="Crop Storage module"
                className={`fixed top-0 left-0 z-40 flex h-dvh w-60 min-w-60 flex-col bg-primary px-4 py-5
                    transition-transform duration-300 md:static md:translate-x-0
                    ${isOpen ? "translate-x-0" : "-translate-x-full"}`}
            >
                <div className="flex flex-col items-center gap-2 px-2">
                    <img src="/logo.webp" alt="" width="150" />
                    <p className="font-heading text-center text-xs font-bold tracking-widest text-white uppercase">
                        Crop Storage
                    </p>
                </div>

                <div className="my-4 h-px w-full bg-white/20" />

                <ul className="flex flex-col gap-1">
                    {NAV_ITEMS.map(({ label, path, icon: Icon, end }) => (
                        <li key={path}>
                            <NavLink to={path} end={end} className={linkStyles}>
                                <Icon size={17} className="shrink-0" />
                                {label}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>
        </>
    );
}
