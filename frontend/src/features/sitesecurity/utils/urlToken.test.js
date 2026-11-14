import { describe, expect, it, vi } from "vitest";
import { hashWithoutToken, readTokenFromHash, stripTokenFromUrl } from "./urlToken";

describe("readTokenFromHash", () => {
    it.each([
        ["#token=abc123", "abc123"],
        ["token=abc123", "abc123"],
        ["#x=1&token=a-b_c", "a-b_c"],
        ["#token=a%2Bb", "a+b"],
    ])("reads %s", (hash, token) => {
        expect(readTokenFromHash(hash)).toBe(token);
    });

    it.each([[""], ["#"], ["#token="], ["#other=1"], [null], [undefined]])("finds no token in %j", (hash) => {
        expect(readTokenFromHash(hash)).toBeNull();
    });
});

describe("hashWithoutToken", () => {
    it("removes the token and keeps the rest", () => {
        expect(hashWithoutToken("#token=abc")).toBe("");
        expect(hashWithoutToken("#x=1&token=abc")).toBe("#x=1");
    });
});

describe("stripTokenFromUrl", () => {
    const history = () => ({ state: { idx: 1 }, replaceState: vi.fn() });

    it("replaces the history entry without the token", () => {
        const h = history();

        const changed = stripTokenFromUrl({ pathname: "/sitesecurity/activate", search: "?a=1", hash: "#token=abc" }, h);

        expect(changed).toBe(true);
        expect(h.replaceState).toHaveBeenCalledWith({ idx: 1 }, "", "/sitesecurity/activate?a=1");
    });

    it("does nothing when there is no token", () => {
        const h = history();

        expect(stripTokenFromUrl({ pathname: "/p", search: "", hash: "" }, h)).toBe(false);
        expect(h.replaceState).not.toHaveBeenCalled();
    });
});
