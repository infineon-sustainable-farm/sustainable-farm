import { StrictMode, Suspense } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { router } from "./app/router";
import QueryProvider from "./app/QueryProvider";
import "./shared/i18n/i18n";
import "./index.css";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <QueryProvider>
      {/* A language other than English loads on demand: the first render waits for it. */}
      <Suspense fallback={null}>
        <RouterProvider router={router} />
      </Suspense>
    </QueryProvider>
  </StrictMode>
);  