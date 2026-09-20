import VisitorManagementLayout from "./components/VisitorManagementLayout";
import DashboardPage from "./components/DashboardPage";

export default [
    {
        path: "visitormanagement",
        element: <VisitorManagementLayout />,
        children: [
            { index: true, element: <DashboardPage /> },
        ],
    },
];