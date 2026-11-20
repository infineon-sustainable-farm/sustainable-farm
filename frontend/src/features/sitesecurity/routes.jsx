import { lazyScreen } from "../../shared/utils/lazyScreen";

// Loaded on first visit only: the module (excluded from the integration, A12) brings
// framer-motion and tailwind-merge, which no other screen needs at startup.
const SiteSecurityApp = lazyScreen(() => import("./components/SiteSecurityApp"));

export default [
  {
    path: "sitesecurity",
    element: <SiteSecurityApp />,
  },
];
