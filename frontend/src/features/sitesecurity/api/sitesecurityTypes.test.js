import { describe, expect, it } from "vitest";
import { LEVEL_META, LEVEL_ORDER, levelMeta } from "./sitesecurityTypes";

describe("levelMeta", () => {
  it("returns the metadata of a known level", () => {
    expect(levelMeta("critical")).toBe(LEVEL_META.critical);
  });

  it.each([["new-zone"], [undefined], [null]])("does not break on the unknown level %s", (level) => {
    expect(levelMeta(level).id).toBe("unknown");
    expect(levelMeta(level).meter).toBe(0);
  });

  it("uses design tokens only", () => {
    for (const level of LEVEL_ORDER) {
      expect(LEVEL_META[level].solid).toMatch(/^var\(--color-/);
      expect(LEVEL_META[level].fill).toMatch(/^var\(--color-/);
    }
  });
});
