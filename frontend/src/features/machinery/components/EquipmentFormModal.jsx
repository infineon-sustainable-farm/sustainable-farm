import { useState } from "react";
import Modal from "../../../shared/components/Modal";
import { CATEGORY, STAGE, STATUS } from "../constants";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const initialForm = {
  name: "",
  category: CATEGORY.AGRICULTURAL_MACHINERY,
  stage: STAGE.CULTIVATION,
  status: STATUS.OPERATIONAL,
};

function EquipmentFormModal({ isOpen, onClose, onSubmit, serverError }) {
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState({});

  function handleChange(e) {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  }

  function validate() {
    const errors = {};
    if (!form.name.trim()) errors.name = "Name is required.";
    else if (form.name.length > 30) errors.name = "Name must be 30 characters or fewer.";
    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    onSubmit(form);
  }

  function handleClose() {
    setForm(initialForm);
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">Add equipment</h2>

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
          <label className="text-sm font-medium">Category</label>
          <select name="category" value={form.category} onChange={handleChange} className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1">
            {Object.values(CATEGORY).map((value) => (
              <option key={value} value={value}>{formatEnumLabel(value)}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-sm font-medium">Stage</label>
          <select name="stage" value={form.stage} onChange={handleChange} className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1">
            {Object.values(STAGE).map((value) => (
              <option key={value} value={value}>{formatEnumLabel(value)}</option>
            ))}
          </select>
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
export default EquipmentFormModal;