import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";

const initialForm = {
  equipmentId: "",
  date: "",
  liters: "",
  cost: "",
};

function toFormState(fuelLog) {
  if (!fuelLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: fuelLog.equipmentId != null ? String(fuelLog.equipmentId) : "",
    date: fuelLog.date ?? "",
    liters: fuelLog.liters != null ? String(fuelLog.liters) : "",
    cost: fuelLog.cost != null ? String(fuelLog.cost) : "",
  };
}

function FuelLogFormModal({
  isOpen,
  fuelLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(fuelLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(fuelLog);
  const canLeaveEquipmentEmpty = !isEditing || !fuelLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = "Equipment is required.";
    if (!form.date) errors.date = "Date is required.";

    if (!form.liters.trim()) errors.liters = "Liters is required.";
    else if (!NON_NEGATIVE_DECIMAL.test(form.liters.trim())) {
      errors.liters = "Liters must be 0 or more with up to 2 decimals.";
    }

    if (!form.cost.trim()) errors.cost = "Cost is required.";
    else if (!NON_NEGATIVE_DECIMAL.test(form.cost.trim())) {
      errors.cost = "Cost must be 0 or more with up to 2 decimals.";
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
      liters: form.liters.trim(),
      cost: form.cost.trim(),
    });
  }

  function handleClose() {
    setForm(toFormState(fuelLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit fuel log" : "Add fuel log"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      {!hasEquipment && !isEquipmentLoading && (
        <p className="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded mb-4">
          No equipment available. Add equipment before logging fuel.
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
            <label className="text-sm font-medium">Liters</label>
            <input
              type="number"
              name="liters"
              min="0"
              step="0.01"
              value={form.liters}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.liters && <p className="text-red-600 text-xs mt-1">{fieldErrors.liters}</p>}
          </div>
        </div>

        <div>
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
export default FuelLogFormModal;
