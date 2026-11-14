import { describe, expect, it } from "vitest";
import { cleanSignInCode, isValidSignInCode } from "./signInCode";

describe("isValidSignInCode", () => {
    it("accepts 6-digit and 10-digit codes", () => {
        expect(isValidSignInCode("123456")).toBe(true);
        expect(isValidSignInCode("1234567890")).toBe(true);
    });

    it("keeps leading zeros", () => {
        expect(isValidSignInCode("000042")).toBe(true);
        expect(isValidSignInCode("0000000042")).toBe(true);
        expect(cleanSignInCode("0000 000 042")).toBe("0000000042");
    });

    it("rejects every other length", () => {
        for (const code of ["", "12345", "1234567", "12345678", "123456789", "12345678901"]) {
            expect(isValidSignInCode(code)).toBe(false);
        }
    });

    it("rejects anything but digits, and ignores spaces", () => {
        expect(isValidSignInCode("12345a")).toBe(false);
        expect(isValidSignInCode("12 34 56")).toBe(true);
        expect(isValidSignInCode(null)).toBe(false);
    });
});
