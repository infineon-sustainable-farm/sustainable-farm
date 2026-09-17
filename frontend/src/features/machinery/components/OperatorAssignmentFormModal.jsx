import { useState } from "react";
import Modal from "../../../shared/components/Modal";

const MAX_TEXT_LENGTH = 60;

function toFormState(assignment) {
  return {
    fullName: assignment?.fullName ?? "",
    jobTitle: assignment?.jobTitle ?? "",
    equipmentId: assignment?.equipmentId != null ? String(assignment.equipmentId) : "",
    startDate: assignment?.startDate ?? "",
    endDate: assignment?.endDate ?? "",
  };
}

function OperatorAssignmentFormModal({
  isOpen,
  assignment,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const [form, setForm] = useState(() => toFormState(assignment));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(assignment);
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const fullName = form.fullName.trim();
    const jobTitle = form.jobTitle.trim();

    if (!fullName) errors.fullName = "Operator's full name is required.";
    else if (fullName.length > MAX_TEXT_LENGTH) errors.fullName = `Full name must be ${MAX_TEXT_LENGTH} characters or fewer.`;

    if (!jobTitle) errors.jobTitle = "Job title is required.";
    else if (jobTitle.length > MAX_TEXT_LENGTH) errors.jobTitle = `Job title must be ${MAX_TEXT_LENGTH} characters or fewer.`;

    if (!form.equipmentId) errors.equipmentId = "Equipment is required.";
    if (!form.startDate) errors.startDate = "Start date is required.";
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
      errors.endDate = "End date cannot be before start date.";
    }

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      fullName: form.fullName.trim(),
      jobTitle: form.jobTitle.trim(),
      equipmentId: Number(form.equipmentId),
      startDate: form.startDate,
      endDate: form.endDate || null,
    });
  }

  function handleClose() {
    setForm(toFormState(assignment));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <h2 className="text-lg font-bold mb-4">
        {isEditing ? "Edit operator assignment" : "Assign operator"}
      </h2>

      {serverError && (
        <p className="bg-red-50 text-red-700 text-sm px-3 py-2 rounded mb-4">{serverError}</p>
      )}

      {!hasEquipment && !isEquipmentLoading && (
        <p className="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded mb-4">
          No equipment available. Add equipment before assigning an operator.
        </p>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <div>
          <label className="text-sm font-medium">Operator's full name</label>
          <input
            type="text"
            name="fullName"
            value={form.fullName}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
          />
          {fieldErrors.fullName && <p className="text-red-600 text-xs mt-1">{fieldErrors.fullName}</p>}
        </div>

        <div>
          <label className="text-sm font-medium">Job title</label>
          <input
            type="text"
            name="jobTitle"
            value={form.jobTitle}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
          />
          {fieldErrors.jobTitle && <p className="text-red-600 text-xs mt-1">{fieldErrors.jobTitle}</p>}
        </div>

        <div>
          <label className="text-sm font-medium">Equipment</label>
          <select
            name="equipmentId"
            value={form.equipmentId}
            onChange={handleChange}
            disabled={isEquipmentLoading}
            className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1 disabled:bg-gray-50"
          >
            <option value="">
              {isEquipmentLoading ? "Loading equipment…" : "Select equipment"}
            </option>
            {equipments.map((equipment) => (
              <option key={equipment.id} value={equipment.id}>{equipment.name}</option>
            ))}
          </select>
          {fieldErrors.equipmentId && <p className="text-red-600 text-xs mt-1">{fieldErrors.equipmentId}</p>}
        </div>

        <div className="flex gap-3">
          <div className="flex-1">
            <label className="text-sm font-medium">Start date</label>
            <input
              type="date"
              name="startDate"
              value={form.startDate}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.startDate && <p className="text-red-600 text-xs mt-1">{fieldErrors.startDate}</p>}
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium">End date (optional)</label>
            <input
              type="date"
              name="endDate"
              value={form.endDate}
              onChange={handleChange}
              className="border border-gray-300 rounded-lg w-full px-3 py-2 mt-1"
            />
            {fieldErrors.endDate && <p className="text-red-600 text-xs mt-1">{fieldErrors.endDate}</p>}
          </div>
        </div>

        <div className="flex justify-end gap-2 mt-2">
          <button type="button" onClick={handleClose} className="px-4 py-2 rounded-lg border border-gray-300">
            Cancel
          </button>
          <button
            type="submit"
            disabled={!hasEquipment}
            className="px-4 py-2 rounded-lg bg-primary text-white disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Save
          </button>
        </div>
      </form>
    </Modal>
  );
}
export default OperatorAssignmentFormModal;
