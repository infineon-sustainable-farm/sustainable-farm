import SiteSecurityApp from "./components/SiteSecurityApp";
import ActivateAccount from "./screens/ActivateAccount";
import ResetPassword from "./screens/ResetPassword";

/* The link pages are public siblings of the app, outside its sign-in gate. */
export default [
  {
    path: "sitesecurity",
    element: <SiteSecurityApp />,
  },
  {
    path: "sitesecurity/activate",
    element: <ActivateAccount />,
  },
  {
    path: "sitesecurity/reset-password",
    element: <ResetPassword />,
  },
];
