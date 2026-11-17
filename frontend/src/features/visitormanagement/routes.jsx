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
import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/Screens/DashboardPage";
import SchedulingPage from "./components/Screens/SchedulingPage";
import RegistrationPage from "./components/Screens/RegistrationPage";
import VisitorsPage from "./components/Screens/VisitorsPage";
import EducationPage from "./components/Screens/EducationPage";
import SafetyPage from "./components/Screens/SafetyPage";
import BookingPage from "./components/Screens/BookingPage";
import FeedbackPage from "./components/Screens/FeedbackPage";
import EventsPage from "./components/Screens/EventsPage";
import StaffPage from "./components/Screens/StaffPage";

export default [
    {
        path: "visitormanagement",
        element: <VisitorManagementLayout />,
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
 * page (app/LauncherPage.jsx). Same entries, order and targets as the module's own sidebar.
 */
export const nav = {
    id: "visitormanagement",
    basePath: "/visitormanagement",
    icon: Users,
    nameKey: "visitormanagement:name",
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
