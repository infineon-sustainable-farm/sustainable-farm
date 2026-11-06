import { describe, expect, it } from "vitest";
import { LEVEL_META, levelMeta } from "./sitesecurityTypes";

describe("levelMeta", () => {
    it("returns the metadata of a known level", () => {
        expect(levelMeta("critical")).toBe(LEVEL_META.critical);
    });

    it.each([["new-zone"], [undefined], [null]])("does not break on the unknown level %s", (level) => {
        expect(levelMeta(level).label).toBe("Unknown");
    });
});
