import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { MAINTENANCE_TYPE } from "../constants";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const MAX_FREQUENCY_LENGTH = 100;
const MAX_OPERATOR_LENGTH = 60;

const initialForm = {
  equipmentId: "",
  type: MAINTENANCE_TYPE.PREVENTIVE,
  frequency: "",
  lastCompleted: "",
  nextDue: "",
  operator: "",
};

function toFormState(schedule) {
  if (!schedule) return initialForm;

  return {
    equipmentId: schedule.equipmentId != null ? String(schedule.equipmentId) : "",
    type: schedule.type ?? initialForm.type,
    frequency: schedule.frequency ?? "",
    lastCompleted: schedule.lastCompleted ?? "",
    nextDue: schedule.nextDue ?? "",
    operator: schedule.operator ?? "",
  };
}

function MaintenanceScheduleFormModal({
  isOpen,
  schedule,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(schedule));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(schedule);
  const canLeaveEquipmentEmpty = !isEditing || !schedule?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = "Equipment is required.";
    if (!form.lastCompleted) errors.lastCompleted = "Last completed date is required.";
    if (!form.nextDue) errors.nextDue = "Next due date is required.";
    else if (form.lastCompleted && form.nextDue < form.lastCompleted) {
      errors.nextDue = "Next due date cannot be before the last completed date.";
    }
    if (form.frequency.trim().length > MAX_FREQUENCY_LENGTH) {
      errors.frequency = `Frequency must be ${MAX_FREQUENCY_LENGTH} characters or fewer.`;
    }
    if (form.operator.trim().length > MAX_OPERATOR_LENGTH) {
      errors.operator = `Operator must be ${MAX_OPERATOR_LENGTH} characters or fewer.`;
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
      type: form.type,
      frequency: form.frequency.trim() || null,
      lastCompleted: form.lastCompleted,
      nextDue: form.nextDue,
      operator: form.operator.trim() || null,
    });
  }

  function handleClose() {
    setForm(toFormState(schedule));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit maintenance schedule" : "Add maintenance schedule"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      {!hasEquipment && !isEquipmentLoading && (
        <p className="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded mb-4">
          No equipment available. Add equipment before scheduling maintenance.
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
            <label className="text-sm font-medium">Type</label>
            <select name="type" value={form.type} onChange={handleChange} className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1">
              {Object.values(MAINTENANCE_TYPE).map((value) => (
                <option key={value} value={value}>{formatEnumLabel(value)}</option>
              ))}
            </select>
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">Frequency</label>
            <input
              type="text"
              name="frequency"
              value={form.frequency}
              onChange={handleChange}
              placeholder="e.g. Every 250 hours"
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.frequency && <p className="text-red-600 text-xs mt-1">{fieldErrors.frequency}</p>}
          </div>
        </div>

        <div className="flex gap-3">
          <div className="flex-1">
            <label className="text-sm font-medium">Last completed</label>
            <input
              type="date"
              name="lastCompleted"
              value={form.lastCompleted}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.lastCompleted && <p className="text-red-600 text-xs mt-1">{fieldErrors.lastCompleted}</p>}
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">Next due date</label>
            <input
              type="date"
              name="nextDue"
              value={form.nextDue}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.nextDue && <p className="text-red-600 text-xs mt-1">{fieldErrors.nextDue}</p>}
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
export default MaintenanceScheduleFormModal;
