import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../shared/api/client";
import {
    createCredentialApi,
    fetchAccessLogs,
    fetchCredentials,
    fetchSecurityOverview,
    fetchZones,
    requestSignInCode,
    restoreSession,
    revokeCredentialApi,
    signOut,
    verifySignInCode,
} from "./sitesecurityApi";
import { SITESECURITY_ENDPOINTS } from "./endpoints";
import { endSession, getSessionEmail, getSessionRole, setSessionEndedHandler, startSession } from "./sitesecurityAuth";

vi.mock("../../../shared/api/client", () => ({
    apiClient: { get: vi.fn(), post: vi.fn() },
}));

const WITH_COOKIE = { withCredentials: true };

describe("sitesecurityApi", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        apiClient.get.mockResolvedValue([]);
        apiClient.post.mockResolvedValue({});
        setSessionEndedHandler(null);
        endSession();
        startSession("admin", "admin@example.test");
    });

    it("has a real URL for every endpoint", () => {
        for (const url of Object.values(SITESECURITY_ENDPOINTS)) {
            expect(url).toMatch(/^\/api\/v1\/sitesecurity\/[a-z]+(\/[a-z-]+)?$/);
        }
    });

    it.each([
        ["overview", fetchSecurityOverview, "/api/v1/sitesecurity/overview"],
        ["zones", fetchZones, "/api/v1/sitesecurity/zones"],
        ["credentials", fetchCredentials, "/api/v1/sitesecurity/credentials"],
    ])("reads %s with the session cookie and no token", async (_name, call, url) => {
        await call();

        expect(apiClient.get).toHaveBeenCalledWith(url, WITH_COOKIE);
    });

    it("sends the log filter", async () => {
        await fetchAccessLogs("denied");

        expect(apiClient.get).toHaveBeenCalledWith("/api/v1/sitesecurity/logs", {
            params: { filter: "denied" },
            withCredentials: true,
        });
    });

    it("creates a credential", async () => {
        await createCredentialApi({ name: "Ada" });

        expect(apiClient.post).toHaveBeenCalledWith("/api/v1/sitesecurity/credentials", { name: "Ada" }, WITH_COOKIE);
    });

    it("escapes the id when revoking a credential", async () => {
        await revokeCredentialApi("a/b c");

        expect(apiClient.post).toHaveBeenCalledWith(
            "/api/v1/sitesecurity/credentials/a%2Fb%20c/revoke",
            null,
            WITH_COOKIE,
        );
    });

    it("keeps the validation details of a failed creation", async () => {
        const failure = Object.assign(new Error("Validation failed"), {
            status: 400,
            data: { fieldErrors: { name: "Name is required" } },
        });
        apiClient.post.mockRejectedValue(failure);

        await expect(createCredentialApi({})).rejects.toBe(failure);
    });

    it("asks for a sign-in code without opening a session", async () => {
        endSession();

        await requestSignInCode("op@example.test");

        expect(apiClient.post).toHaveBeenCalledWith(
            "/api/v1/sitesecurity/auth/request-code",
            { email: "op@example.test" },
            WITH_COOKIE,
        );
        expect(getSessionRole()).toBeNull();
    });

    it("opens a session when the server accepts the code", async () => {
        endSession();
        apiClient.post.mockResolvedValue({ role: "read", email: "op@example.test" });

        await expect(verifySignInCode("op@example.test", "123456")).resolves.toBe("read");

        expect(apiClient.post).toHaveBeenCalledWith(
            "/api/v1/sitesecurity/auth/verify",
            { email: "op@example.test", code: "123456" },
            WITH_COOKIE,
        );
        expect(getSessionRole()).toBe("read");
        expect(getSessionEmail()).toBe("op@example.test");
    });

    it("does not open a session when the code is refused", async () => {
        endSession();
        apiClient.post.mockRejectedValue(Object.assign(new Error("no"), { status: 401 }));

        await expect(verifySignInCode("op@example.test", "000000")).rejects.toThrow("no");

        expect(getSessionRole()).toBeNull();
    });

    it("restores a session that is still open", async () => {
        endSession();
        apiClient.get.mockResolvedValue({ role: "admin", email: "admin@example.test" });

        await expect(restoreSession()).resolves.toBe("admin");

        expect(apiClient.get).toHaveBeenCalledWith("/api/v1/sitesecurity/session", WITH_COOKIE);
        expect(getSessionRole()).toBe("admin");
    });

    it("signs out on the server and locks the screen", async () => {
        const ended = vi.fn();
        setSessionEndedHandler(ended);

        await signOut();

        expect(apiClient.post).toHaveBeenCalledWith("/api/v1/sitesecurity/auth/logout", null, WITH_COOKIE);
        expect(ended).toHaveBeenCalledOnce();
        expect(getSessionRole()).toBeNull();
    });

    it("locks the screen even when the server cannot be reached", async () => {
        apiClient.post.mockRejectedValue(new Error("offline"));

        await expect(signOut()).rejects.toThrow("offline");

        expect(getSessionRole()).toBeNull();
    });

    it.each([
        ["read", () => fetchZones(), "get"],
        ["create", () => createCredentialApi({}), "post"],
        ["revoke", () => revokeCredentialApi("id"), "post"],
    ])("ends the session when a %s call gets a 401", async (_name, call, method) => {
        const ended = vi.fn();
        setSessionEndedHandler(ended);
        apiClient[method].mockRejectedValue(Object.assign(new Error("expired"), { status: 401 }));

        await expect(call()).rejects.toThrow("expired");

        expect(ended).toHaveBeenCalledOnce();
        expect(getSessionRole()).toBeNull();
    });

    it("keeps the session on a 403", async () => {
        apiClient.post.mockRejectedValue(Object.assign(new Error("admin only"), { status: 403 }));

        await expect(createCredentialApi({})).rejects.toThrow("admin only");

        expect(getSessionRole()).toBe("admin");
    });
});
