import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/Screens/DashboardPage";
import SchedulingPage from "./components/Screens/SchedulingPage";
import RegistrationPage from "./components/Screens/RegistrationPage";
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
            { path: "education", element: <EducationPage /> },
            { path: "safety", element: <SafetyPage /> },
            { path: "booking", element: <BookingPage /> },
            { path: "feedback", element: <FeedbackPage /> },
            { path: "events", element: <EventsPage /> },
            { path: "staff", element: <StaffPage /> },
        ],
    },
];