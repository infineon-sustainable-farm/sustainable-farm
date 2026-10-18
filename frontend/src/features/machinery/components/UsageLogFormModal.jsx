import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";

const MAX_OPERATOR_LENGTH = 60;
const MAX_NOTES_LENGTH = 500;

const initialForm = {
  equipmentId: "",
  date: "",
  hoursUsed: "",
  operator: "",
  notes: "",
};

function toFormState(usageLog) {
  if (!usageLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: usageLog.equipmentId != null ? String(usageLog.equipmentId) : "",
    date: usageLog.date ?? "",
    hoursUsed: usageLog.hoursUsed != null ? String(usageLog.hoursUsed) : "",
    operator: usageLog.operator ?? "",
    notes: usageLog.notes ?? "",
  };
}

function UsageLogFormModal({
  isOpen,
  usageLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(usageLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(usageLog);
  const canLeaveEquipmentEmpty = !isEditing || !usageLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = "Equipment is required.";
    if (!form.date) errors.date = "Date is required.";

    if (!form.hoursUsed.trim()) errors.hoursUsed = "Hours used is required.";
    else if (!NON_NEGATIVE_DECIMAL.test(form.hoursUsed.trim())) {
      errors.hoursUsed = "Hours used must be 0 or more with up to 2 decimals.";
    }

    if (form.operator.trim().length > MAX_OPERATOR_LENGTH) {
      errors.operator = `Operator must be ${MAX_OPERATOR_LENGTH} characters or fewer.`;
    }
    if (form.notes.trim().length > MAX_NOTES_LENGTH) {
      errors.notes = `Notes must be ${MAX_NOTES_LENGTH} characters or fewer.`;
    }

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      equipmentId: form.equipmentId ? Number(form.equipmentId) : null,
      date: form.date,
      hoursUsed: form.hoursUsed.trim(),
      operator: form.operator.trim() || null,
      notes: form.notes.trim() || null,
    });
  }

  function handleClose() {
    setForm(toFormState(usageLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit usage log" : "Add usage log"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      {!hasEquipment && !isEquipmentLoading && (
        <p className="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded mb-4">
          No equipment available. Add equipment before logging usage.
        </p>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <div>
          <label className="text-sm font-medium">Equipment</label>
          <select
            name="equipmentId"
            value={form.equipmentId}
            onChange={handleChange}
            disabled={isEquipmentLoading}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1 disabled:bg-gray-50"
          >
            {canLeaveEquipmentEmpty && (
              <option value="">
                {isEquipmentLoading ? "Loading equipment…" : "No equipment (detached)"}
              </option>
            )}
            {equipments.map((equipment) => (
              <option key={equipment.id} value={equipment.id}>{equipment.name}</option>
            ))}
          </select>
          {fieldErrors.equipmentId && <p className="text-red-600 text-xs mt-1">{fieldErrors.equipmentId}</p>}
        </div>

        <div className="flex gap-3">
          <div className="flex-1">
            <label className="text-sm font-medium">Date</label>
            <input
              type="date"
              name="date"
              value={form.date}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.date && <p className="text-red-600 text-xs mt-1">{fieldErrors.date}</p>}
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">Hours used</label>
            <input
              type="number"
              name="hoursUsed"
              min="0"
              step="0.1"
              value={form.hoursUsed}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.hoursUsed && <p className="text-red-600 text-xs mt-1">{fieldErrors.hoursUsed}</p>}
          </div>
        </div>

        <div>
          <label className="text-sm font-medium">Operator</label>
          <input
            type="text"
            name="operator"
            value={form.operator}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
          />
          {fieldErrors.operator && <p className="text-red-600 text-xs mt-1">{fieldErrors.operator}</p>}
        </div>

        <div>
          <label className="text-sm font-medium">Notes</label>
          <textarea
            name="notes"
            rows="3"
            value={form.notes}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1 resize-none"
          />
          {fieldErrors.notes && <p className="text-red-600 text-xs mt-1">{fieldErrors.notes}</p>}
        </div>

        <div className="flex justify-end gap-2 mt-2">
          <button type="button" onClick={handleClose} className="px-4 py-2 rounded-lg border border-gray-300">
            Cancel
          </button>
          <button type="submit" className="px-4 py-2 rounded-lg bg-primary text-white">
            Save
          </button>
        </div>
      </form>
    </Modal>
  );
}
export default UsageLogFormModal;
