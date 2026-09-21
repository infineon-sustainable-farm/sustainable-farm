import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/DashboardPage";
import SchedulingPage from "./components/SchedulingPage";
import RegistrationPage from "./components/RegistrationPage";
import EducationPage from "./components/EducationPage";
import SafetyPage from "./components/SafetyPage";
import BookingPage from "./components/BookingPage";
import FeedbackPage from "./components/FeedbackPage";

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
        ],
    },
];