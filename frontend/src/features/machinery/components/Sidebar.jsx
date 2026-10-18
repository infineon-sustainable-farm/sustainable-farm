import { useState } from "react";
import { Menu, X, LayoutDashboard, Cog, Wrench, Fuel, ClipboardList, Users, Boxes, LogOut } from "lucide-react";
import { NavLink } from "react-router-dom";
import LowStockNotifications from "./LowStockNotifications";

const navItems = [
    { label: "Dashboard", icon: LayoutDashboard, path: "/machinery" },
    { label: "Equipment Registry", icon: Cog, path: "/machinery/equipment-registry" },
    { label: "Maintenance", icon: Wrench, path: "/machinery/maintenance" },
    { label: "Usage & Fuel Log", icon: Fuel, path: "/machinery/usage-fuel-log" },
    { label: "Repair Log", icon: ClipboardList, path: "/machinery/repair-log" },
    { label: "Operators", icon: Users, path: "/machinery/operators" },
    { label: "Spare Parts", icon: Boxes, path: "/machinery/spare-parts" },
];

const linkStyles = ({ isActive }) =>
    `flex items-center gap-3 h-9 w-full px-3 rounded-lg cursor-pointer text-sm text-white transition-colors ${isActive
        ? "bg-[color-mix(in_srgb,var(--color-primary)_70%,white_30%)]"
        : "hover:bg-[color-mix(in_srgb,var(--color-primary)_85%,white_15%)]"
    }`; 

const logoutStyles =
    "flex items-center gap-3 h-9 w-full px-3 rounded-lg cursor-pointer text-sm text-white transition-colors hover:bg-[color-mix(in_srgb,var(--color-primary)_85%,white_15%)]";

function Sidebar() {
    const [isOpen, setIsOpen] = useState(false);

    return (
        <>
            <button
                type="button"
                className="md:hidden fixed top-4 left-4 z-50 text-white bg-primary p-2 rounded-lg"
                onClick={() => setIsOpen(!isOpen)}
            >
                {isOpen ? <X /> : <Menu />}
            </button>

            <nav
                className={`bg-primary w-60 h-dvh flex flex-col pt-5 px-3
          fixed md:static top-0 left-0 z-40
          transition-transform duration-300
          ${isOpen ? "translate-x-0" : "-translate-x-full"} md:translate-x-0`}
            >
                <div className="flex flex-col items-center gap-2 px-2">
                    <img src="/logo.webp" alt="Logo" className="w-20 h-auto" />
                    <h3 className="text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-white/75 text-center">
                        Machinery Management
                    </h3>
                </div>

                <div className="bg-white/20 h-px w-4/5 my-4 mx-auto"></div>

                <ul className="w-full flex flex-col gap-1">
                    {navItems.map(({ label, icon: Icon, path }) => (
                        <li key={label}>
                            <NavLink to={path} className={linkStyles} end={path === "/machinery"}>
                                <Icon size={18} />
                                {label}
                            </NavLink>
                        </li>
                    ))}
                </ul>

                <div className="mt-auto flex flex-col gap-1 mb-[max(1.5rem,env(safe-area-inset-bottom))]">
                    <LowStockNotifications />
                    <button
                        type="button"
                        className={logoutStyles}
                    >
                        <LogOut size={18} />
                        Logout
                    </button>
                </div>
            </nav>
        </>
    );
}
export default Sidebar;