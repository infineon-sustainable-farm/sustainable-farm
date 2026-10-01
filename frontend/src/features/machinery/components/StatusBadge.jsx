import { STATUS } from "../constants";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const statusColors = {
  [STATUS.OPERATIONAL]: "bg-green-100 text-green-700",
  [STATUS.UNDER_MAINTENANCE]: "bg-amber-100 text-amber-700",
  [STATUS.OUT_OF_SERVICE]: "bg-red-100 text-red-700",
};

function StatusBadge({ status }) {
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-semibold ${statusColors[status]}`}>
      {formatEnumLabel(status)}
    </span>
  );
}
export default StatusBadge;