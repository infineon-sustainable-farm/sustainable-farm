import { useLocation } from "react-router-dom";

/*
 * Screen title per route, matching the mockup's top-bar wording. Unmatched
 * paths fall back to the module name.
 */
const TITLES = {
    "/visitormanagement": "Dashboard Overview",
    "/visitormanagement/scheduling": "Farm Tour Scheduling",
    "/visitormanagement/registration": "Visitor Registration",
    "/visitormanagement/visitors": "Visitors",
    "/visitormanagement/education": "Educational Program",
    "/visitormanagement/safety": "Safety Briefing Tracker",
    "/visitormanagement/booking": "Agritourism Booking System",
    "/visitormanagement/feedback": "Satisfaction Survey",
    "/visitormanagement/events": "Events",
    "/visitormanagement/staff": "Staff & Guides",
};

/**
 * The sticky white bar of the mockup: it carries the current screen's title so
 * the user always knows where they are. The right-hand "user chip" of the
 * mockup is left out on purpose — there is no authentication to fill it.
 */
export default function ModuleTopBar() {
    const { pathname } = useLocation();
    return (
        <header className="sticky top-0 z-20 flex shrink-0 items-center justify-between border-b border-line bg-white px-8 py-4">
            <h1 className="font-heading text-xl font-bold text-primary-dark">
                {TITLES[pathname] ?? "Visitor Management"}
            </h1>
        </header>
    );
}