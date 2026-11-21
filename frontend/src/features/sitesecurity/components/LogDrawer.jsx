import { useTranslation } from "react-i18next";
import { Button, Modal } from "../../../shared/ui";
import { Avatar, LevelBadge, StatusLabel } from "./ui";
import { translateEnum } from "../utils/status";

const TYPE_GROUP = "type";
const ACTION_GROUP = "action";

function Row({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-6 py-3">
      <dt className="text-xs font-medium uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="text-right text-sm font-medium text-text">{children}</dd>
    </div>
  );
}

/* Details of one access event. The person is a reference to the shared directory. */
export default function LogDrawer({ entry, onClose }) {
  const { t } = useTranslation("sitesecurity");
  const { t: tCommon } = useTranslation("common");
  const unidentified = !entry?.personId;
  const typeLabel = entry ? translateEnum(t, TYPE_GROUP, entry.type) : "";
  const actionLabel = entry ? translateEnum(t, ACTION_GROUP, entry.action) : "";
  return (
    <Modal
      open={Boolean(entry)}
      onClose={onClose}
      title={entry ? t("log.detail.title", { ref: entry.ref }) : ""}
      description={entry ? t(`log.detail.outcome.${entry.status === "Denied" ? "denied" : entry.status === "Approved" ? "approved" : "other"}`) : ""}
      size="md"
      footer={
        <Button variant="secondary" onClick={onClose}>
          {tCommon("actions.close")}
        </Button>
      }
    >
      {entry && (
        <div>
          <div className="flex items-center gap-3 rounded-tile bg-surface-muted px-4 py-3">
            <Avatar initials={entry.initials} level={unidentified ? undefined : entry.zone} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-text">{entry.user ?? t("log.unidentified")}</p>
              <p className="text-xs text-text-muted">{typeLabel}</p>
            </div>
            <StatusLabel status={entry.status} />
          </div>
          <dl className="mt-2 divide-y divide-border-subtle">
            <Row label={t("log.columns.date")}>{entry.date}</Row>
            <Row label={t("log.columns.time")}>{entry.time}</Row>
            <Row label={t("log.columns.zone")}>
              <span className="flex items-center justify-end gap-2">
                {entry.zoneName}
                <LevelBadge level={entry.zone} />
              </span>
            </Row>
            <Row label={t("log.columns.action")}>
              {t("log.detail.actionAt", { action: actionLabel, gate: entry.gate })}
            </Row>
            <Row label={t("log.detail.method")}>{entry.method}</Row>
            <Row label={t("log.detail.note")}>{entry.note}</Row>
          </dl>
        </div>
      )}
    </Modal>
  );
}
