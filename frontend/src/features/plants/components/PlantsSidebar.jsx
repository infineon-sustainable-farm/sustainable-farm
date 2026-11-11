import { useState } from "react";
import {
    Apple,
    Bug,
    CalendarDays,
    FlaskConical,
    Home,
    Leaf,
    Menu,
    Sprout,
    TrendingUp,
    X,
} from "lucide-react";
import { NavLink } from "react-router-dom";

/*
 * The eight entries of the approved mock-up, in its order and with its wording,
 * each leading to its screen. The Overview is the home of the module, at
 * /plants itself.
 */
const NAV_ITEMS = [
    { label: "Overview", icon: Home, path: "/plants" },
    { label: "Varieties", icon: Sprout, path: "/plants/varieties" },
    { label: "Growth Calendar", icon: CalendarDays, path: "/plants/growth-calendar" },
    { label: "Fertilizer Inventory", icon: FlaskConical, path: "/plants/fertilizer-inventory" },
    { label: "Diseases & Alerts", icon: Bug, path: "/plants/diseases-alerts" },
    { label: "Harvest & Maturity", icon: Apple, path: "/plants/harvest-maturity" },
    { label: "Nursery", icon: Leaf, path: "/plants/nursery" },
    { label: "Scenario Analysis", icon: TrendingUp, path: "/plants/scenario-analysis" },
];

// 36 px entries with 18 px icons, the sizes of the Machinery sidebar.
const ITEM_BASE =
    "flex h-9 items-center gap-2 rounded-md px-3 text-sm text-white focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-white";
const ICON_SIZE = 18;

/**
 * Active entry: a light dark tint, the mock-up's left marker in 2 px and a
 * semibold label, rather than a solid block. Every state keeps white text at
 * AA or more: #0a8276 bar -> 4.70:1; hover and active, black at 10% over it
 * (#09756a) -> 5.58:1. Lightening the row instead, as the mock-up does, gave
 * 3.64:1. Colours stay white and black over the existing --color-primary
 * token; no new hue is introduced.
 */
const linkStyles = ({ isActive }) =>
    `${ITEM_BASE} hover:bg-black/10 ${isActive ? "bg-black/10 font-semibold shadow-[inset_2px_0_0_#fff]" : "font-normal"}`;

/**
 * Module shell navigation, following the Machinery sidebar: same width, same
 * off-canvas behaviour under md, same lucide icons.
 *
 * Three things of the mock-up are left out on purpose: the Logout button (there
 * is no authentication), the "Online · Banfora" status and the farm name under
 * the module title (both would hard-code farm data into the interface).
 */
export default function PlantsSidebar() {
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
                aria-label="Plants module"
                className={`fixed top-0 left-0 z-40 flex h-dvh w-60 min-w-60 flex-col bg-primary px-4 py-4
                    transition-transform duration-300 md:static md:translate-x-0
                    ${isOpen ? "translate-x-0" : "-translate-x-full"}`}
            >
                <div className="flex flex-col items-center gap-1 px-2">
                    {/* The application's logo, served from public/ as machinery serves it, at
                        Machinery's 80 px. Empty alt: the module name follows immediately, in text. */}
                    <img src="/logo.webp" alt="" width="80" height="80" />
                    <p className="font-heading text-center text-xs font-bold tracking-widest text-white uppercase">
                        Plants
                    </p>
                </div>

                <div className="my-4 h-px w-full bg-white/20" />

                <ul className="flex flex-col gap-1">
                    {/* Following a link closes the menu on a phone, the current screen's link included;
                        above md the bar is always shown and closing changes nothing. */}
                    {NAV_ITEMS.map(({ label, icon: Icon, path }) => (
                        <li key={label}>
                            <NavLink
                                to={path}
                                end={path === "/plants"}
                                className={linkStyles}
                                onClick={() => setIsOpen(false)}
                            >
                                <Icon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
                                {label}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>
        </>
    );
}
