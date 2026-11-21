import { useTranslation } from "react-i18next";
import { groupAlertsByPriority } from "../utils/alertPriority";

/**
 * A list of alerts in two groups, "Act now" then "Plan ahead", each under its
 * name and its count. Inside a group the alerts keep the order of the API, the
 * most urgent first; a group without alert is not shown.
 *
 * `idPrefix` names the group headings, `listClassName` styles each list, and
 * `renderAlert` gives the list item of one alert.
 */
export default function PlantAlertGroups({ alerts, idPrefix, listClassName, renderAlert }) {
    const { t } = useTranslation("plants");
    return (
        <div className="flex flex-col gap-4">
            {groupAlertsByPriority(alerts).map((group) => {
                const headingId = `${idPrefix}-${group.priority.toLowerCase().replace("_", "-")}`;
                return (
                    <div key={group.priority} className="flex flex-col gap-2">
                        <h3 id={headingId} className="text-sm font-semibold text-text">
                            {group.label}{" "}
                            <span className="text-xs font-normal text-text-muted">
                                {t("alertGroups.count", { count: group.alerts.length })}
                            </span>
                        </h3>
                        <ul aria-labelledby={headingId} className={listClassName}>
                            {group.alerts.map(renderAlert)}
                        </ul>
                    </div>
                );
            })}
        </div>
    );
}
