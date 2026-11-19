import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from "../../../shared/api/client";
import { MACHINERY_ENDPOINTS } from "./endpoints";
import { createEquipment, deleteEquipment, fetchEquipments, updateEquipmentStatus } from "./equipmentApi";
import {
  createOperatorAssignment,
  deleteOperatorAssignment,
  fetchOperatorAssignments,
  updateOperatorAssignment,
} from "./operatorAssignmentApi";

beforeEach(() => {
  vi.clearAllMocks();
});

describe("machinery api paths", () => {
  it("puts every endpoint under /api/machinery", () => {
    for (const path of Object.values(MACHINERY_ENDPOINTS)) {
      expect(path.startsWith("/api/machinery/")).toBe(true);
    }
  });

  it("calls the equipment register", () => {
    fetchEquipments(2);
    createEquipment({ name: "Tractor" });
    updateEquipmentStatus(7, "UNDER_MAINTENANCE");
    deleteEquipment(7);

    expect(apiClient.get).toHaveBeenCalledWith("/api/machinery/equipment", { params: { page: 2, size: 10 } });
    expect(apiClient.post).toHaveBeenCalledWith("/api/machinery/equipment", { name: "Tractor" });
    expect(apiClient.patch).toHaveBeenCalledWith("/api/machinery/equipment/7", { status: "UNDER_MAINTENANCE" });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/machinery/equipment/7");
  });

  it("calls the operator assignments", () => {
    fetchOperatorAssignments(0, 5);
    createOperatorAssignment({ jobTitle: "Driver" });
    updateOperatorAssignment(3, { jobTitle: "Lead" });
    deleteOperatorAssignment(3);

    expect(apiClient.get).toHaveBeenCalledWith("/api/machinery/operator-assignments", { params: { page: 0, size: 5 } });
    expect(apiClient.post).toHaveBeenCalledWith("/api/machinery/operator-assignments", { jobTitle: "Driver" });
    expect(apiClient.patch).toHaveBeenCalledWith("/api/machinery/operator-assignments/3", { jobTitle: "Lead" });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/machinery/operator-assignments/3");
  });
});
