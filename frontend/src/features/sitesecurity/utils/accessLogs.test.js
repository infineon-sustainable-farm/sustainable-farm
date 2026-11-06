import { describe, expect, it } from "vitest";
import { countLogs, filterByZone, recentEvents } from "./accessLogs";

const LOGS = [
    { id: "1", status: "Approved", type: "Staff", zone: "open" },
    { id: "2", status: "Denied", type: "Unknown", zone: "critical" },
    { id: "3", status: "Approved", type: "Visitor", zone: "open" },
    { id: "4", status: "Approved", type: "Service", zone: "controlled" },
    { id: "5", status: "Denied", type: "Staff", zone: "critical" },
];

describe("countLogs", () => {
    it("counts each filter", () => {
        expect(countLogs(LOGS)).toEqual({ all: 5, approved: 3, denied: 2, visitors: 1, staff: 2, service: 1 });
    });

    it("handles an empty log", () => {
        expect(countLogs([]).all).toBe(0);
    });
});

describe("filterByZone", () => {
    it("keeps only the events of the zone", () => {
        expect(filterByZone(LOGS, "critical").map((e) => e.id)).toEqual(["2", "5"]);
    });

    it("keeps everything without a zone", () => {
        expect(filterByZone(LOGS, null)).toBe(LOGS);
    });
});

describe("recentEvents", () => {
    it("keeps the first four events, in the order of the API", () => {
        expect(recentEvents(LOGS).map((e) => e.id)).toEqual(["1", "2", "3", "4"]);
    });

    it("does not depend on any specific id", () => {
        expect(recentEvents([{ id: "any-id" }])).toEqual([{ id: "any-id" }]);
    });
});
