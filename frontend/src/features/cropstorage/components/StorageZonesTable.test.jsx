// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import StorageZonesTable from "./StorageZonesTable";

afterEach(cleanup);

describe("storage zones table capacity usage", () => {
    it.each([
        [69.999, "69.999%", "rgb(22, 163, 74)"],
        [70, "70%", "rgb(245, 158, 11)"],
        [90, "90%", "rgb(245, 158, 11)"],
        [90.004, "90.004%", "rgb(239, 68, 68)"],
    ])("displays %s%% with the matching threshold color", (usedPercent, label, color) => {
        render(
            <StorageZonesTable
                zones={[
                    {
                        id: 1,
                        zoneCode: "A",
                        name: "Zone A",
                        capacityKg: 100,
                        capacityM3: 1,
                        usedPercent,
                    },
                ]}
            />,
        );

        expect(screen.getByText(label)).toBeTruthy();
        const progress = screen.getByRole("progressbar", { name: "A capacity used" });
        expect(progress.getAttribute("aria-valuetext")).toBe(`${label} used`);
        expect(progress.firstElementChild.style.background).toBe(color);
    });
});
