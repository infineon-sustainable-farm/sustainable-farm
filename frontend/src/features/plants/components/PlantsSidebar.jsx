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
 * The eight entries of the approved mock-up, in its order and with its wording.
 * Only the two screens that exist carry a path; the six others are listed to show
 * the module's plan and are deliberately inert — see the rendering below.
 */
const NAV_ITEMS = [
    { label: "Overview", icon: Home },
    { label: "Varieties", icon: Sprout, path: "/plants" },
    { label: "Growth Calendar", icon: CalendarDays, path: "/plants/growth-calendar" },
    { label: "Fertilizer Inventory", icon: FlaskConical },
    { label: "Diseases & Alerts", icon: Bug },
    { label: "Harvest & Maturity", icon: Apple },
    { label: "Nursery", icon: Leaf },
    { label: "Scenario Analysis", icon: TrendingUp },
];

const ITEM_BASE = "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm";

/**
 * Active entry: a dark overlay plus the mock-up's inset left marker. The mock-up
 * lightens the row instead, which pushes its background towards its own white
 * label — 3.64:1, under the AA minimum. Darkening raises the label to 6.66:1 and
 * detaches the row from the bar. Colours stay white and black over the existing
 * --color-primary token; no new hue is introduced.
 */
const linkStyles = ({ isActive }) =>
    `${ITEM_BASE} font-medium text-white hover:bg-white/10 ${
        isActive ? "bg-black/20 font-semibold shadow-[inset_3px_0_0_#fff]" : ""
    }`;

/**
 * An entry whose screen does not exist yet. Rendered as plain text, so it is not
 * focusable and not clickable at all, and marked aria-disabled with a note for
 * screen readers — the greying alone would say nothing to them.
 */
function PlannedItem({ label, icon: Icon }) {
    return (
        <span
            aria-disabled="true"
            className={`${ITEM_BASE} cursor-not-allowed font-normal text-white/70`}
        >
            <Icon size={17} className="shrink-0" />
            {label}
            <span className="sr-only"> (planned, not available yet)</span>
        </span>
    );
}

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
                className={`fixed top-0 left-0 z-40 flex h-dvh w-60 min-w-60 flex-col bg-primary px-4 py-5
                    transition-transform duration-300 md:static md:translate-x-0
                    ${isOpen ? "translate-x-0" : "-translate-x-full"}`}
            >
                <div className="flex flex-col items-center gap-2 px-2">
                    {/* The application's logo, served from public/ as machinery serves it.
                        Empty alt: the module name follows immediately, in text. */}
                    <img src="/logo.webp" alt="" width="150" />
                    <p className="font-heading text-center text-xs font-bold tracking-widest text-white uppercase">
                        Plants
                    </p>
                </div>

                <div className="my-4 h-px w-full bg-white/20" />

                <ul className="flex flex-col gap-1">
                    {NAV_ITEMS.map(({ label, icon: Icon, path }) => (
                        <li key={label}>
                            {path ? (
                                <NavLink to={path} end={path === "/plants"} className={linkStyles}>
                                    <Icon size={17} className="shrink-0" />
                                    {label}
                                </NavLink>
                            ) : (
                                <PlannedItem label={label} icon={Icon} />
                            )}
                        </li>
                    ))}
                </ul>
            </nav>
        </>
    );
}
