function parseLocalDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function DueBadge({ nextDue }) {
  if (!nextDue) return null;

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = parseLocalDate(nextDue);
  const diff = Math.round((due - today) / 86400000);

  if (diff < 0) {
    return (
      <span className="inline-block rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
        Overdue by {Math.abs(diff)}d
      </span>
    );
  }
  if (diff <= 7) {
    return (
      <span className="inline-block rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
        Due in {diff}d
      </span>
    );
  }
  return (
    <span className="inline-block rounded-full bg-gray-100 px-2 py-0.5 text-xs font-semibold text-gray-600">
      In {diff}d
    </span>
  );
}
export default DueBadge;
