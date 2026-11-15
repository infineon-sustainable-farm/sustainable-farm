function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <p className="text-red-600 mb-4">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="border border-gray-300 px-4 py-2 rounded-lg hover:bg-gray-50"
        >
          Try again
        </button>
      )}
    </div>
  );
}
export default ErrorState;
