import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";

const MAX_ISSUE_LENGTH = 500;
const MAX_TECHNICIAN_LENGTH = 60;

const initialForm = {
  equipmentId: "",
  date: "",
  issue: "",
  downtime: "",
  cost: "",
  technician: "",
};

function toFormState(repairLog) {
  if (!repairLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: repairLog.equipmentId != null ? String(repairLog.equipmentId) : "",
    date: repairLog.date ?? "",
    issue: repairLog.issue ?? "",
    downtime: repairLog.downtime != null ? String(repairLog.downtime) : "",
    cost: repairLog.cost != null ? String(repairLog.cost) : "",
    technician: repairLog.technician ?? "",
  };
}

function RepairLogFormModal({
  isOpen,
  repairLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(repairLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(repairLog);
  const canLeaveEquipmentEmpty = !isEditing || !repairLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const issue = form.issue.trim();

    if (!form.equipmentId && !isEditing) errors.equipmentId = "Equipment is required.";
    if (!form.date) errors.date = "Date is required.";

    if (!issue) errors.issue = "Issue description is required.";
    else if (issue.length > MAX_ISSUE_LENGTH) {
      errors.issue = `Issue description must be ${MAX_ISSUE_LENGTH} characters or fewer.`;
    }

    if (form.downtime.trim() && !NON_NEGATIVE_DECIMAL.test(form.downtime.trim())) {
      errors.downtime = "Downtime must be 0 or more with up to 2 decimals.";
    }
    if (form.cost.trim() && !NON_NEGATIVE_DECIMAL.test(form.cost.trim())) {
      errors.cost = "Cost must be 0 or more with up to 2 decimals.";
    }
    if (form.technician.trim().length > MAX_TECHNICIAN_LENGTH) {
      errors.technician = `Technician must be ${MAX_TECHNICIAN_LENGTH} characters or fewer.`;
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
      issue: form.issue.trim(),
      downtime: form.downtime.trim() || null,
      cost: form.cost.trim() || null,
      technician: form.technician.trim() || null,
    });
  }

  function handleClose() {
    setForm(toFormState(repairLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit repair log" : "Add repair log"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      {!hasEquipment && !isEquipmentLoading && (
        <p className="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded mb-4">
          No equipment available. Add equipment before logging a repair.
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
            <label className="text-sm font-medium">Technician</label>
            <input
              type="text"
              name="technician"
              value={form.technician}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.technician && <p className="text-red-600 text-xs mt-1">{fieldErrors.technician}</p>}
          </div>
        </div>

        <div>
          <label className="text-sm font-medium">Issue description</label>
          <textarea
            name="issue"
            rows="3"
            value={form.issue}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1 resize-none"
          />
          {fieldErrors.issue && <p className="text-red-600 text-xs mt-1">{fieldErrors.issue}</p>}
        </div>

        <div className="flex gap-3">
          <div className="flex-1">
            <label className="text-sm font-medium">Downtime (hours)</label>
            <input
              type="number"
              name="downtime"
              min="0"
              step="0.1"
              value={form.downtime}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.downtime && <p className="text-red-600 text-xs mt-1">{fieldErrors.downtime}</p>}
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">Cost</label>
            <input
              type="number"
              name="cost"
              min="0"
              step="0.01"
              value={form.cost}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.cost && <p className="text-red-600 text-xs mt-1">{fieldErrors.cost}</p>}
          </div>
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
export default RepairLogFormModal;
