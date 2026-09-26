function EmptyState({ message, actionLabel, onAction }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center text-gray-500">
      <p className="mb-4">{message}</p>
      {actionLabel && (
        <button type="button" onClick={onAction} className="bg-primary text-white px-4 py-2 rounded-lg">
          {actionLabel}
        </button>
      )}
    </div>
  );
}
export default EmptyState;