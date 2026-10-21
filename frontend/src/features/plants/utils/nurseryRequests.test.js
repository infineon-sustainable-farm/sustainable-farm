import { describe, expect, it } from "vitest";
import { batchRequestBody, IN_HOUSE, PURCHASED } from "./nurseryRequests";

// A form filled in full, including fields a purchase only would send.
const FILLED_FORM = {
    batchCode: "P2",
    varietyName: "Keitt",
    origin: PURCHASED,
    supplier: "Pépinière Banfora",
    supplierLotNumber: "L-2026-07",
    startedOn: "2026-09-20",
    initialCount: "200",
    initialStage: "HARDENING",
    plannedTransplantOn: "2026-11-09",
    plannedBlockCode: "F",
};

describe("batchRequestBody", () => {
    it("sends no supplier for plants grown on the farm, even if the fields still hold one", () => {
        const body = batchRequestBody({ ...FILLED_FORM, origin: IN_HOUSE });

        expect(body.origin).toBe(IN_HOUSE);
        expect(body).not.toHaveProperty("supplier");
        expect(body).not.toHaveProperty("supplierLotNumber");
    });

    it("sends the supplier alone for a purchase without a lot number", () => {
        const body = batchRequestBody({ ...FILLED_FORM, supplierLotNumber: "   " });

        expect(body.supplier).toBe("Pépinière Banfora");
        expect(body).not.toHaveProperty("supplierLotNumber");
    });

    it("sends the supplier and its lot number for a purchase with both", () => {
        const body = batchRequestBody(FILLED_FORM);

        expect(body.supplier).toBe("Pépinière Banfora");
        expect(body.supplierLotNumber).toBe("L-2026-07");
    });

    it("leaves out an empty planned block, and sends one that is given", () => {
        expect(batchRequestBody({ ...FILLED_FORM, plannedBlockCode: " " })).not.toHaveProperty("plannedBlockCode");
        expect(batchRequestBody(FILLED_FORM).plannedBlockCode).toBe("F");
    });

    it("sends the count as a number and no farm", () => {
        const body = batchRequestBody({ ...FILLED_FORM, initialCount: " 150 " });

        expect(body.initialCount).toBe(150);
        expect(body).not.toHaveProperty("farmId");
        expect(Object.keys(body).sort()).toEqual([
            "batchCode",
            "initialCount",
            "initialStage",
            "origin",
            "plannedBlockCode",
            "plannedTransplantOn",
            "startedOn",
            "supplier",
            "supplierLotNumber",
            "varietyName",
        ]);
    });
});
