import { useState } from "react";
import {
    BookOpen,
    Calendar,
    LayoutDashboard,
    LogOut,
    Menu,
    MessageSquare,
    PartyPopper,
    ShieldCheck,
    Ticket,
    UserPlus,
    X,
} from "lucide-react";
import { NavLink } from "react-router-dom";

/*
 * The eight screens of the approved mockup, in its order and with its wording.
 * Only the screens that exist carry a path; the others are listed to show the
 * module's scope and are deliberately inert — see the rendering below. Each new
 * screen activates its entry as it is built.
 */
const NAV_ITEMS = [
    { label: "Dashboard", icon: LayoutDashboard, path: "/visitormanagement" },
    { label: "Scheduling", icon: Calendar, path: "/visitormanagement/scheduling" },
    { label: "Registration", icon: UserPlus, path: "/visitormanagement/registration" },
    { label: "Education", icon: BookOpen, path: "/visitormanagement/education" },
    { label: "Safety", icon: ShieldCheck, path: "/visitormanagement/safety" },
    { label: "Booking", icon: Ticket, path: "/visitormanagement/booking" },
    { label: "Feedback", icon: MessageSquare },
    { label: "Events", icon: PartyPopper },
];

const ITEM_BASE = "flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm";

/*
 * Active entry: the mockup's white overlay — a translucent white wash plus a
 * slightly heavier weight. Colours stay white over the existing --color-primary
 * token; no new hue is introduced.
 */
const linkStyles = ({ isActive }) =>
    `${ITEM_BASE} w-full font-medium text-white/85 hover:bg-white/10 ${
        isActive ? "bg-white/15 font-semibold text-white" : ""
    }`;

/**
 * An entry whose screen does not exist yet. Rendered as plain text, so it is
 * not focusable and not clickable at all, and marked aria-disabled with a note
 * for screen readers — the greying alone would say nothing to them.
 */
function PlannedItem({ label, icon: Icon }) {
    return (
        <span
            aria-disabled="true"
            className={`${ITEM_BASE} w-full cursor-not-allowed font-medium text-white/70`}
        >
            <Icon size={17} className="shrink-0" />
            {label}
            <span className="sr-only"> (planned, not available yet)</span>
        </span>
    );
}

/**
 * Module shell navigation, following the approved mockup: the farm logo in a
 * white circle, the module name, the eight screen entries and the logout
 * button, which is deliberately inert — there is no authentication.
 */
export default function VisitorManagementSidebar() {
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
                aria-label="Visitor Management module"
                className={`fixed top-0 left-0 z-40 flex h-dvh w-[250px] min-w-[250px] flex-col bg-primary px-3.5 py-5
                    transition-transform duration-300 md:translate-x-0
                    ${isOpen ? "translate-x-0" : "-translate-x-full"}`}
            >
                <div className="mx-auto mb-4 flex h-28 w-28 items-center justify-center rounded-full bg-white shadow-lg">
                    {/* The application's logo. Empty alt: the module name follows
                        immediately, in text below. */}
                    <img src="/logo.webp" alt="" className="h-4/5 w-4/5 rounded-full" />
                </div>

                <p className="font-heading mb-2.5 border-b border-white/25 px-1.5 pb-3.5 text-center text-sm font-bold tracking-widest text-white uppercase">
                    Visitor Management
                </p>

                <ul className="flex flex-1 flex-col gap-0.5">
                    {NAV_ITEMS.map(({ label, icon: Icon, path }) => (
                        <li key={label}>
                            {path ? (
                                <NavLink
                                    to={path}
                                    end={path === "/visitormanagement"}
                                    className={linkStyles}
                                >
                                    <Icon size={17} className="shrink-0" />
                                    {label}
                                </NavLink>
                            ) : (
                                <PlannedItem label={label} icon={Icon} />
                            )}
                        </li>
                    ))}
                </ul>

                <button
                    type="button"
                    aria-disabled="true"
                    className="mt-3 flex w-full items-center gap-2.5 rounded-lg bg-white/10 px-3 py-2.5 text-sm font-medium text-white/85"
                >
                    <LogOut size={17} className="shrink-0" />
                    Logout
                    <span className="sr-only"> (not available until authentication)</span>
                </button>
            </nav>
        </>
    );
}