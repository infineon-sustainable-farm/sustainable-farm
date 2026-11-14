import { describe, expect, it } from "vitest";
import { alertPriority, countActNow, groupAlertsByPriority } from "./alertPriority";

const alert = (type, severity = "WARNING", id = 1) => ({ type, severity, id });

describe("alertPriority", () => {
    it("acts now on an open health problem and on a low stock, whatever their severity", () => {
        expect(alertPriority(alert("OPEN_HEALTH_ISSUE"))).toBe("ACT_NOW");
        expect(alertPriority(alert("LOW_STOCK"))).toBe("ACT_NOW");
        expect(alertPriority(alert("LOW_STOCK", "CRITICAL"))).toBe("ACT_NOW");
    });

    it("plans ahead a pre-harvest interval, unless it is critical", () => {
        expect(alertPriority(alert("PRE_HARVEST_INTERVAL"))).toBe("PLAN_AHEAD");
        expect(alertPriority(alert("PRE_HARVEST_INTERVAL", "CRITICAL"))).toBe("ACT_NOW");
    });

    it("plans ahead a batch to transplant and a harvest season", () => {
        expect(alertPriority(alert("NURSERY_READY"))).toBe("PLAN_AHEAD");
        expect(alertPriority(alert("HARVEST_APPROACHING"))).toBe("PLAN_AHEAD");
    });

    it("acts now on every critical alert, an unknown type included", () => {
        expect(alertPriority(alert("SOMETHING_NEW", "CRITICAL"))).toBe("ACT_NOW");
        expect(alertPriority(alert("SOMETHING_NEW"))).toBe("PLAN_AHEAD");
    });
});

describe("groupAlertsByPriority", () => {
    // The order of the API on the demo data: by date, the undated stock alert last.
    const demo = [
        alert("OPEN_HEALTH_ISSUE", "WARNING", 1),
        alert("NURSERY_READY", "WARNING", 2),
        alert("PRE_HARVEST_INTERVAL", "WARNING", 3),
        alert("LOW_STOCK", "WARNING", 4),
    ];

    it("puts Act now first and keeps the order inside each group", () => {
        const groups = groupAlertsByPriority(demo);
        expect(groups.map((group) => group.label)).toEqual(["Act now", "Plan ahead"]);
        expect(groups[0].alerts.map((item) => item.id)).toEqual([1, 4]);
        expect(groups[1].alerts.map((item) => item.id)).toEqual([2, 3]);
    });

    it("keeps every alert once", () => {
        const groups = groupAlertsByPriority(demo);
        expect(groups.flatMap((group) => group.alerts).length).toBe(demo.length);
    });

    it("leaves out a group without alert", () => {
        expect(groupAlertsByPriority([alert("NURSERY_READY")]).map((group) => group.priority)).toEqual(["PLAN_AHEAD"]);
        expect(groupAlertsByPriority([])).toEqual([]);
        expect(groupAlertsByPriority(undefined)).toEqual([]);
    });
});

describe("countActNow", () => {
    it("counts the alerts to act on now", () => {
        expect(countActNow([alert("OPEN_HEALTH_ISSUE"), alert("NURSERY_READY"), alert("PRE_HARVEST_INTERVAL", "CRITICAL")])).toBe(2);
        expect(countActNow([])).toBe(0);
        expect(countActNow(undefined)).toBe(0);
    });
});
