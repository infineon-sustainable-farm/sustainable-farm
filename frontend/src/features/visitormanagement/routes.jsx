import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/DashboardPage";
import SchedulingPage from "./components/SchedulingPage";

export default [
    {
        path: "visitormanagement",
        element: <VisitorManagementLayout />,
        children: [
            { index: true, element: <DashboardPage /> },
            { path: "scheduling", element: <SchedulingPage /> },
        ],
    },
];