import { useEffect } from "react";
import { useTranslation } from "react-i18next";

/** Browser tab title of a Crop Storage screen, "<screen> — Crop Storage", translated. */
export function usePageTitle(titleKey) {
    const { t } = useTranslation("cropstorage");
    const title = t("pageTitle", { screen: t(titleKey) });
    useEffect(() => {
        document.title = title;
    }, [title]);
}
