import { describe, expect, it } from "vitest";
import { createFormatters } from "./format";

describe("machinery formatters", () => {
  it("keeps the English formats machinery had before the translation", () => {
    const format = createFormatters("en", "—");
    expect(format.date("2026-03-05")).toBe("05/03/2026");
    expect(format.date(null)).toBe("—");
    expect(format.date("2026-03")).toBe("2026-03");
    expect(format.decimal(1234.5)).toBe("1234.50");
    expect(format.decimal(7, 0)).toBe("7");
    expect(format.decimal("abc")).toBe("—");
    expect(format.currency(1234.5)).toBe("$1234.50");
    expect(format.currency(undefined)).toBe("—");
    expect(format.currencyTick(150)).toBe("$150");
    expect(format.longDate(new Date(2026, 10, 17))).toBe("Tuesday, November 17, 2026");
    expect(format.month(new Date(2026, 10, 1))).toBe("Nov");
  });

  it("follows the German and French conventions", () => {
    const de = createFormatters("de", "—");
    expect(de.date("2026-03-05")).toBe("05.03.2026");
    expect(de.decimal(1234.5)).toBe("1234,50");
    expect(de.currency(12.5)).toMatch(/^12,50\s\$$/);

    const fr = createFormatters("fr", "—");
    expect(fr.date("2026-03-05")).toBe("05/03/2026");
    expect(fr.currency(12.5)).toMatch(/^12,50\s\$$/);
  });
});
