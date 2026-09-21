import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/DashboardPage";
import SchedulingPage from "./components/SchedulingPage";
import RegistrationPage from "./components/RegistrationPage";

export default [
    {
        path: "visitormanagement",
        element: <VisitorManagementLayout />,
        children: [
            { index: true, element: <DashboardPage /> },
            { path: "scheduling", element: <SchedulingPage /> },
            { path: "registration", element: <RegistrationPage /> },
        ],
    },
];