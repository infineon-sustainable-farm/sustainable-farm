import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { STATUS } from "../constants";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

function StatusUpdateModal({ equipment, onClose, onSubmit }) {
  const [status, setStatus] = useState(equipment?.status ?? STATUS.OPERATIONAL);

  if (!equipment) return null;

  return (
    <Modal isOpen={true} onClose={onClose}>
      <h2 className="text-lg font-bold mb-4">Update status — {equipment.name}</h2>
      <select value={status} onChange={(e) => setStatus(e.target.value)} className="border border-gray-300 rounded-lg w-full px-3 py-2 mb-4">
        {Object.values(STATUS).map((value) => (
          <option key={value} value={value}>{formatEnumLabel(value)}</option>
        ))}
      </select>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg border border-gray-300">Cancel</button>
        <button type="button" onClick={() => onSubmit(equipment.id, status)} className="px-4 py-2 rounded-lg bg-primary text-white">Save</button>
      </div>
    </Modal>
  );
}
export default StatusUpdateModal;