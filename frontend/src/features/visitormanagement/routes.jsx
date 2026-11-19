import {
    BookOpen,
    Briefcase,
    Calendar,
    LayoutDashboard,
    MessageSquare,
    PartyPopper,
    ShieldCheck,
    Ticket,
    UserPlus,
    Users,
} from "lucide-react";
import { lazyScreen } from "../../shared/utils/lazyScreen";

// Screens load on first visit, one chunk each.
const DashboardPage = lazyScreen(() => import("./components/Screens/DashboardPage"));
const SchedulingPage = lazyScreen(() => import("./components/Screens/SchedulingPage"));
const RegistrationPage = lazyScreen(() => import("./components/Screens/RegistrationPage"));
const VisitorsPage = lazyScreen(() => import("./components/Screens/VisitorsPage"));
const EducationPage = lazyScreen(() => import("./components/Screens/EducationPage"));
const SafetyPage = lazyScreen(() => import("./components/Screens/SafetyPage"));
const BookingPage = lazyScreen(() => import("./components/Screens/BookingPage"));
const FeedbackPage = lazyScreen(() => import("./components/Screens/FeedbackPage"));
const EventsPage = lazyScreen(() => import("./components/Screens/EventsPage"));
const StaffPage = lazyScreen(() => import("./components/Screens/StaffPage"));

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below.
export default [
    {
        path: "visitormanagement",
        children: [
            { index: true, element: <DashboardPage /> },
            { path: "scheduling", element: <SchedulingPage /> },
            { path: "registration", element: <RegistrationPage /> },
            { path: "visitors", element: <VisitorsPage /> },
            { path: "education", element: <EducationPage /> },
            { path: "safety", element: <SafetyPage /> },
            { path: "booking", element: <BookingPage /> },
            { path: "feedback", element: <FeedbackPage /> },
            { path: "events", element: <EventsPage /> },
            { path: "staff", element: <StaffPage /> },
        ],
    },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Visitor management renders inside the shell.
 */
export const nav = {
    id: "visitormanagement",
    basePath: "/visitormanagement",
    icon: Users,
    nameKey: "visitormanagement:name",
    shell: true,
    items: [
        { to: "/visitormanagement", labelKey: "visitormanagement:nav.dashboard", icon: LayoutDashboard, end: true },
        { to: "/visitormanagement/scheduling", labelKey: "visitormanagement:nav.scheduling", icon: Calendar },
        { to: "/visitormanagement/registration", labelKey: "visitormanagement:nav.registration", icon: UserPlus },
        { to: "/visitormanagement/visitors", labelKey: "visitormanagement:nav.visitors", icon: Users },
        { to: "/visitormanagement/education", labelKey: "visitormanagement:nav.education", icon: BookOpen },
        { to: "/visitormanagement/safety", labelKey: "visitormanagement:nav.safety", icon: ShieldCheck },
        { to: "/visitormanagement/booking", labelKey: "visitormanagement:nav.booking", icon: Ticket },
        { to: "/visitormanagement/feedback", labelKey: "visitormanagement:nav.feedback", icon: MessageSquare },
        { to: "/visitormanagement/events", labelKey: "visitormanagement:nav.events", icon: PartyPopper },
        { to: "/visitormanagement/staff", labelKey: "visitormanagement:nav.staff", icon: Briefcase },
    ],
};
