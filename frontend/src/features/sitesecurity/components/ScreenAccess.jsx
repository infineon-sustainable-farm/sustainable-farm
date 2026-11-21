import { useTranslation } from "react-i18next";
import { Lock } from "lucide-react";
import { EmptyState } from "../../../shared/ui";
import { useSiteSecurityAccess } from "../hooks/useSiteSecurityAccess";

/* Renders the screen only when the caller's permissions open it. */
export default function ScreenAccess({ screen, children }) {
  const { t } = useTranslation("sitesecurity");
  const { screens } = useSiteSecurityAccess();
  if (!screens.includes(screen)) {
    return (
      <div className="p-6">
        <EmptyState icon={Lock} title={t("gate.notPermittedTitle")} message={t("gate.notPermitted")} />
      </div>
    );
  }
  return children;
}
