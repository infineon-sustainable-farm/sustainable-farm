// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { StorageGauge } from "./DashboardVisuals";

afterEach(cleanup);

function renderGauge(percentage, usedKg, capacityKg) {
    render(
        <StorageGauge
            percentage={percentage}
            usedKg={usedKg}
            capacityKg={capacityKg}
            zones={[{ id: 1, name: "Zone A", capacityKg, currentQuantityKg: usedKg }]}
        />,
    );
}

describe("storage capacity gauge", () => {
    it.each([
        ["missing percentage", null, 20, 100],
        ["invalid percentage", "not-a-number", 20, 100],
        ["missing quantity", 20, null, 100],
        ["blank quantity", 20, "", 100],
        ["invalid quantity", 20, "bad", 100],
        ["missing capacity", 20, 20, null],
        ["blank capacity", 20, 20, ""],
        ["invalid capacity", 20, 20, "bad"],
    ])("shows an unknown neutral state for %s", (_label, percentage, usedKg, capacityKg) => {
        renderGauge(percentage, usedKg, capacityKg);

        expect(screen.getAllByText("—").length).toBeGreaterThan(0);
        const gauge = screen.getByRole("img", { name: "Overall storage usage unavailable" });
        expect(gauge.children[1].getAttribute("stroke")).toBe("#dbe4ec");
        const progress = screen.getByRole("progressbar", { name: "Zone A capacity" });
        if (
            usedKg == null ||
            !Number.isFinite(Number(usedKg)) ||
            capacityKg == null ||
            !Number.isFinite(Number(capacityKg))
        ) {
            expect(progress.hasAttribute("aria-valuenow")).toBe(false);
            expect(progress.getAttribute("aria-valuetext")).toBe("Usage unavailable");
            expect(progress.firstElementChild.style.background).toBe("rgb(219, 228, 236)");
        }
    });

    it("shows a real zero rather than treating it as missing", () => {
        renderGauge(0, 0, 100);

        expect(screen.getAllByText("0%")).toHaveLength(2);
        expect(screen.getByRole("progressbar", { name: "Zone A capacity" }).getAttribute("aria-valuenow")).toBe("0");
    });

    it("uses each zone's percentage rather than the overall percentage", () => {
        render(
            <StorageGauge
                percentage={50}
                usedKg={100}
                capacityKg={200}
                zones={[
                    { id: 1, name: "Zone A", capacityKg: 100, currentQuantityKg: 25 },
                    { id: 2, name: "Zone B", capacityKg: 100, currentQuantityKg: 75 },
                ]}
            />,
        );

        expect(screen.getByRole("progressbar", { name: "Zone A capacity" }).getAttribute("aria-valuenow")).toBe("25");
        expect(screen.getByRole("progressbar", { name: "Zone B capacity" }).getAttribute("aria-valuenow")).toBe("75");
    });

    it.each([
        [53.67, "rgb(22, 163, 74)"],
        [70, "rgb(245, 158, 11)"],
        [90, "rgb(245, 158, 11)"],
        [95, "rgb(239, 68, 68)"],
    ])("uses the zone's own percentage and convention at %s%%", (percentage, color) => {
        renderGauge(percentage, percentage, 100);

        expect(screen.getAllByText(`${percentage}%`).length).toBeGreaterThan(0);
        expect(screen.getByRole("progressbar", { name: "Zone A capacity" }).firstElementChild.style.background).toBe(
            color,
        );
    });

    it.each([
        [69.999, "69.999%", "rgb(22, 163, 74)"],
        [90.004, "90.004%", "rgb(239, 68, 68)"],
    ])("preserves boundary precision for %s%%", (percentage, displayed, color) => {
        renderGauge(percentage, percentage, 100);

        expect(screen.getByRole("img", { name: `Overall storage usage ${displayed.slice(0, -1)} percent` })).toBeTruthy();
        expect(screen.getAllByText(displayed).length).toBeGreaterThan(0);
        expect(
            screen.getByRole("progressbar", { name: "Zone A capacity" }).firstElementChild.style.background,
        ).toBe(color);
    });

    it("shows the actual value above 100% while bounding the visual and ARIA value", () => {
        renderGauge(150, 150, 100);

        expect(screen.getByRole("img", { name: "Overall storage usage 150 percent" })).toBeTruthy();
        expect(screen.getAllByText("150%").length).toBeGreaterThan(0);
        const progress = screen.getByRole("progressbar", { name: "Zone A capacity" });
        expect(progress.getAttribute("aria-valuenow")).toBe("100");
        expect(progress.getAttribute("aria-valuetext")).toBe("150% used");
        expect(progress.firstElementChild.style.width).toBe("100%");
    });

});
