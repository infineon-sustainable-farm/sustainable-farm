import Modal from "../../../shared/components/Modal";

function ConfirmDeleteRecordDialog({ isOpen, title, message, onClose, onConfirm, serverError }) {
  if (!isOpen) return null;

  return (
    <Modal isOpen={true} onClose={onClose}>
      <h2 className="text-lg font-bold mb-2">{title}</h2>
      <p className="text-sm text-gray-600 mb-4">{message}</p>
      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg border border-gray-300">Cancel</button>
        <button type="button" onClick={onConfirm} className="px-4 py-2 rounded-lg bg-red-600 text-white">Delete</button>
      </div>
    </Modal>
  );
}
export default ConfirmDeleteRecordDialog;
