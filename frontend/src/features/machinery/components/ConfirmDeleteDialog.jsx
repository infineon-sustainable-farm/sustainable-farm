import Modal from "../../../shared/components/Modal";

function ConfirmDeleteDialog({ equipment, onClose, onConfirm }) {
  if (!equipment) return null;

  return (
    <Modal isOpen={true} onClose={onClose}>
      <h2 className="text-lg font-bold mb-2">Delete equipment</h2>
      <p className="text-sm text-gray-600 mb-4">
        Are you sure you want to delete "{equipment.name}"? This cannot be undone.
      </p>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg border border-gray-300">Cancel</button>
        <button type="button" onClick={() => onConfirm(equipment.id)} className="px-4 py-2 rounded-lg bg-red-600 text-white">Delete</button>
      </div>
    </Modal>
  );
}
export default ConfirmDeleteDialog;