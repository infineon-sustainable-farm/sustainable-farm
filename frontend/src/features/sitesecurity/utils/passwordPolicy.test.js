import { describe, expect, it } from "vitest";
import { isNewPasswordReady, isPasswordAcceptable, passwordProblems, passwordRuleChecks } from "./passwordPolicy";

describe("passwordProblems", () => {
    it("accepts a passphrase with spaces", () => {
        expect(passwordProblems("correct horse battery", "op@example.test")).toEqual([]);
    });

    it.each([
        ["", ["min_length"]],
        ["elevenchars", ["min_length"]],
        ["a".repeat(129), ["max_length"]],
    ])("checks the length of %j", (password, failed) => {
        expect(passwordProblems(password)).toEqual(failed);
    });

    it("accepts the length limits", () => {
        expect(passwordProblems("twelve chars")).toEqual([]);
        expect(passwordProblems("b".repeat(128))).toEqual([]);
    });

    it("refuses the email, ignoring case", () => {
        expect(passwordProblems("Ops.Lead@Example.test", "ops.lead@example.test")).toEqual(["not_email"]);
    });

    it("skips the email rule without an email", () => {
        expect(passwordProblems("ops.lead@example.test")).toEqual([]);
    });

    it.each([["password1234"], ["PassWord1234"], ["123456789012"], ["qwertyqwerty"]])("refuses the common password %s", (password) => {
        expect(passwordProblems(password)).toEqual(["not_common"]);
    });

    it("tolerates a missing value", () => {
        expect(passwordProblems(undefined)).toEqual(["min_length"]);
    });
});

describe("passwordRuleChecks", () => {
    it("lists the email rule only when the email is known", () => {
        expect(passwordRuleChecks("x").map((r) => r.id)).toEqual(["min_length", "max_length", "not_common"]);
        expect(passwordRuleChecks("x", "a@b.c").map((r) => r.id)).toContain("not_email");
    });

    it("marks which rules are met", () => {
        const checks = passwordRuleChecks("short");
        expect(checks.find((r) => r.id === "min_length").ok).toBe(false);
        expect(checks.find((r) => r.id === "not_common").ok).toBe(true);
    });
});

describe("isNewPasswordReady", () => {
    it("needs an acceptable password typed twice", () => {
        expect(isPasswordAcceptable("long enough pass")).toBe(true);
        expect(isNewPasswordReady("long enough pass", "long enough pass")).toBe(true);
        expect(isNewPasswordReady("long enough pass", "long enough pas")).toBe(false);
        expect(isNewPasswordReady("short", "short")).toBe(false);
    });
});
