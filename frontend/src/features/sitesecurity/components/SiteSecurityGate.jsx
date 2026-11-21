import { Outlet } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ShieldOff } from "lucide-react";
import { EmptyState, ErrorState, Skeleton } from "../../../shared/ui";
import { useAuthorization } from "../hooks/useAuthorization";
import { visibleScreens } from "../utils/accessRights";

/*
 * Opens the module screens once the server has given the caller's Site Security authorization.
 * The screens read it with useOutletContext() and hide what the caller may not open; the server
 * enforces the same rules on every request.
 */
export default function SiteSecurityGate() {
  const { t } = useTranslation("sitesecurity");
  const { authorization, isLoading, errorStatus } = useAuthorization();

  if (isLoading) {
    return (
      <div className="p-6" role="status" aria-label={t("gate.loading")}>
        <Skeleton className="h-10 w-1/3" />
        <Skeleton className="mt-4 h-40 w-full" radius="card" />
      </div>
    );
  }
  if (errorStatus === 401) {
    return (
      <div className="p-6">
        <EmptyState icon={ShieldOff} title={t("gate.noIdentityTitle")} message={t("gate.noIdentity")} />
      </div>
    );
  }
  if (errorStatus === 403) {
    return (
      <div className="p-6">
        <EmptyState icon={ShieldOff} title={t("gate.noRoleTitle")} message={t("gate.noRole")} />
      </div>
    );
  }
  if (errorStatus !== null || !authorization) {
    return (
      <div className="p-6">
        <ErrorState message={t("gate.error")} onRetry={() => window.location.reload()} />
      </div>
    );
  }

  const screens = visibleScreens(authorization);
  if (screens.length === 0) {
    return (
      <div className="p-6">
        <EmptyState icon={ShieldOff} title={t("gate.noScreensTitle")} message={t("gate.noScreens")} />
      </div>
    );
  }
  return <Outlet context={{ authorization, screens }} />;
}
