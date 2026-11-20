import { describe, expect, it } from "vitest";
import { createFormatters, formatEnumValue } from "./format";

describe("energy supply formatters", () => {
  it("keeps the English output of the screens before the translation", () => {
    const format = createFormatters("en");
    expect(format.number(1234.5)).toBe("1234.5");
    expect(format.fixed(12.25)).toBe("12.3");
    expect(format.fixed(18.5, 2)).toBe("18.50");
    expect(format.grouped(12345.6)).toBe("12,345.6");
    expect(format.date("2026-11-08")).toBe("2026-11-08");
    expect(format.dateTime("2026-11-18T10:15:00")).toBe("2026-11-18T10:15:00");
  });

  it("formats numbers and dates in German and French", () => {
    const de = createFormatters("de");
    expect(de.number(1234.5)).toBe("1234,5");
    expect(de.date("2026-11-08")).toBe("08.11.2026");
    expect(de.dateTime("2026-11-18T10:15:00")).toBe("18.11.26, 10:15");
    const fr = createFormatters("fr");
    expect(fr.fixed(12.25)).toBe("12,3");
    expect(fr.date("2026-11-08")).toBe("08/11/2026");
  });

  it("keeps the old enum label as fallback", () => {
    expect(formatEnumValue("PV_ARRAY")).toBe("PV Array");
    expect(formatEnumValue("UNDER_MAINTENANCE")).toBe("Under Maintenance");
  });
});
