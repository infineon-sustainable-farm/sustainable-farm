import { useState } from "react";
import Modal from "../../../shared/components/Modal";

const MAX_NAME_LENGTH = 255;
const MAX_UNIT_COST = 100000000;
const NON_NEGATIVE_INTEGER = /^\d+$/;
const NON_NEGATIVE_DECIMAL = /^\d+(\.\d{1,2})?$/;

const initialForm = {
  name: "",
  quantity: "",
  reorderThreshold: "",
  unitCost: "",
  equipmentId: "",
};

function toFormState(sparePart) {
  if (!sparePart) return initialForm;

  return {
    name: sparePart.name ?? "",
    quantity: sparePart.quantity != null ? String(sparePart.quantity) : "",
    reorderThreshold: sparePart.reorderThreshold != null ? String(sparePart.reorderThreshold) : "",
    unitCost: sparePart.unitCost != null ? String(sparePart.unitCost) : "",
    equipmentId: sparePart.equipmentId != null ? String(sparePart.equipmentId) : "",
  };
}

function SparePartFormModal({
  isOpen,
  sparePart,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(sparePart));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(sparePart);
  const canUnassignEquipment = !isEditing || !sparePart?.equipmentId;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const name = form.name.trim();

    if (!name) errors.name = "Name is required.";
    else if (name.length > MAX_NAME_LENGTH) errors.name = `Name must be ${MAX_NAME_LENGTH} characters or fewer.`;

    if (!form.quantity.trim()) errors.quantity = "Quantity is required.";
    else if (!NON_NEGATIVE_INTEGER.test(form.quantity.trim())) errors.quantity = "Quantity must be a whole number of 0 or more.";

    if (!form.reorderThreshold.trim()) errors.reorderThreshold = "Reorder threshold is required.";
    else if (!NON_NEGATIVE_INTEGER.test(form.reorderThreshold.trim())) errors.reorderThreshold = "Reorder threshold must be a whole number of 0 or more.";

    if (!form.unitCost.trim()) errors.unitCost = "Unit cost is required.";
    else if (!NON_NEGATIVE_DECIMAL.test(form.unitCost.trim())) errors.unitCost = "Unit cost must be 0 or more with up to 2 decimals.";
    else if (Number(form.unitCost) >= MAX_UNIT_COST) errors.unitCost = "Unit cost is too large.";

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      name: form.name.trim(),
      quantity: Number(form.quantity),
      reorderThreshold: Number(form.reorderThreshold),
      unitCost: form.unitCost.trim(),
      equipmentId: form.equipmentId ? Number(form.equipmentId) : null,
    });
  }

  function handleClose() {
    setForm(toFormState(sparePart));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit spare part" : "Add spare part"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <div>
          <label className="text-sm font-medium">Name</label>
          <input
            type="text"
            name="name"
            value={form.name}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
          />
          {fieldErrors.name && <p className="text-red-600 text-xs mt-1">{fieldErrors.name}</p>}
        </div>

        <div>
          <label className="text-sm font-medium">Compatible asset</label>
          <select
            name="equipmentId"
            value={form.equipmentId}
            onChange={handleChange}
            disabled={isEquipmentLoading}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1 disabled:bg-gray-50"
          >
            {canUnassignEquipment && (
              <option value="">
                {isEquipmentLoading ? "Loading equipment…" : "No compatible asset"}
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
            <label className="text-sm font-medium">Quantity in stock</label>
            <input
              type="number"
              name="quantity"
              min="0"
              step="1"
              value={form.quantity}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.quantity && <p className="text-red-600 text-xs mt-1">{fieldErrors.quantity}</p>}
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">Reorder threshold</label>
            <input
              type="number"
              name="reorderThreshold"
              min="0"
              step="1"
              value={form.reorderThreshold}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.reorderThreshold && <p className="text-red-600 text-xs mt-1">{fieldErrors.reorderThreshold}</p>}
          </div>
        </div>

        <div>
          <label className="text-sm font-medium">Unit cost</label>
          <input
            type="number"
            name="unitCost"
            min="0"
            step="0.01"
            value={form.unitCost}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
          />
          {fieldErrors.unitCost && <p className="text-red-600 text-xs mt-1">{fieldErrors.unitCost}</p>}
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
export default SparePartFormModal;
