import { createBrowserRouter, Navigate } from "react-router-dom";
import RootLayout from "./RootLayout";

const modules = import.meta.glob("../features/*/routes.jsx", { eager: true });

const featureRoutes = Object.values(modules).flatMap((m) => m.default);

export const router = createBrowserRouter([
    {
        path: "/",
        element: <RootLayout />,
        children: [
            { index: true, element: <Navigate to="/salesmarketing/campaigns" replace /> },
            ...featureRoutes,
        ],
    },
]);