import { describe, expect, it } from "vitest";
import { TOO_MANY_ATTEMPTS_MESSAGE, WRONG_ORIGIN_MESSAGE, authErrorMessage, passwordFieldError } from "./authErrors";

describe("authErrorMessage", () => {
    it.each([
        [{ status: 401 }, "bad"],
        [{ status: 403 }, WRONG_ORIGIN_MESSAGE],
        [{ status: 429, message: "<html>" }, TOO_MANY_ATTEMPTS_MESSAGE],
        [{ status: 429, data: "<html>" }, TOO_MANY_ATTEMPTS_MESSAGE],
        [{ status: 429, data: { message: "Too many sign-in attempts. Wait a few minutes." } }, "Too many sign-in attempts. Wait a few minutes."],
        [{ status: 500, message: "boom" }, "boom"],
        [{}, "fallback"],
    ])("maps %j", (err, message) => {
        expect(authErrorMessage(err, "bad", "fallback")).toBe(message);
    });
});

describe("passwordFieldError", () => {
    it("reads the password field error", () => {
        expect(passwordFieldError({ data: { fieldErrors: { password: "Too common" } } })).toBe("Too common");
        expect(passwordFieldError({ data: "html" })).toBeNull();
    });
});
