// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import OperatorAssignmentFormModal from "./OperatorAssignmentFormModal";

vi.mock("../../../shared/api/persons", () => ({
  fetchPersons: vi.fn(() => Promise.resolve([{ id: 4, fullName: "Awa Driver" }])),
}));

const EQUIPMENTS = [{ id: 1, name: "Tractor" }];

function renderModal(props = {}) {
  const onSubmit = vi.fn();
  render(
    <OperatorAssignmentFormModal isOpen equipments={EQUIPMENTS} isEquipmentLoading={false} onClose={vi.fn()}
      onSubmit={onSubmit} {...props} />,
  );
  return onSubmit;
}

describe("OperatorAssignmentFormModal", () => {
  it("assigns a person picked in the people directory, never a typed name", async () => {
    const onSubmit = renderModal();
    await screen.findByRole("option", { name: "Awa Driver" });

    // jsdom enforces the native `required` on click: submit directly to reach the form's own check.
    fireEvent.submit(screen.getByRole("button", { name: "Save" }).closest("form"));
    expect(screen.getByText("Pick the operator.")).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/^Operator/), { target: { value: "4" } });
    fireEvent.change(screen.getByLabelText("Job title"), { target: { value: " Driver " } });
    fireEvent.change(screen.getByLabelText("Equipment"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Start date"), { target: { value: "2026-01-01" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).toHaveBeenCalledWith({
      personId: 4,
      jobTitle: "Driver",
      equipmentId: 1,
      startDate: "2026-01-01",
      endDate: null,
    });
  });

  it("preselects the person of an assignment being edited", async () => {
    renderModal({
      assignment: { id: 2, fullName: "Awa Driver", personId: 4, equipmentId: 1, jobTitle: "Driver", startDate: "2026-01-01" },
    });
    await screen.findByRole("option", { name: "Awa Driver" });
    expect(screen.getByLabelText(/^Operator/)).toHaveValue("4");
  });
});
