import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../shared/api/client";
import {
    createCredentialApi,
    fetchAccessLogs,
    fetchCredentials,
    fetchSecurityOverview,
    fetchZones,
    openSession,
    revokeCredentialApi,
} from "./sitesecurityApi";
import { SITESECURITY_ENDPOINTS } from "./endpoints";
import { endSession, getSessionRole, setSessionEndedHandler, startSession } from "./sitesecurityAuth";

vi.mock("../../../shared/api/client", () => ({
    apiClient: { get: vi.fn(), post: vi.fn() },
}));

const BEARER = { Authorization: "Bearer the-token" };

describe("sitesecurityApi", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        apiClient.get.mockResolvedValue([]);
        apiClient.post.mockResolvedValue({});
        setSessionEndedHandler(null);
        endSession();
        startSession("the-token", "admin");
    });

    it("has a real URL for every endpoint", () => {
        for (const url of Object.values(SITESECURITY_ENDPOINTS)) {
            expect(url).toMatch(/^\/api\/v1\/sitesecurity\/[a-z]+$/);
        }
    });

    it.each([
        ["overview", fetchSecurityOverview, "/api/v1/sitesecurity/overview"],
        ["zones", fetchZones, "/api/v1/sitesecurity/zones"],
        ["credentials", fetchCredentials, "/api/v1/sitesecurity/credentials"],
    ])("reads %s with the token", async (_name, call, url) => {
        await call();

        expect(apiClient.get).toHaveBeenCalledWith(url, { headers: BEARER });
    });

    it("sends the log filter with the token", async () => {
        await fetchAccessLogs("denied");

        expect(apiClient.get).toHaveBeenCalledWith("/api/v1/sitesecurity/logs", {
            params: { filter: "denied" },
            headers: BEARER,
        });
    });

    it("creates a credential with the token", async () => {
        await createCredentialApi({ name: "Ada" });

        expect(apiClient.post).toHaveBeenCalledWith(
            "/api/v1/sitesecurity/credentials",
            { name: "Ada" },
            { headers: BEARER },
        );
    });

    it("escapes the id when revoking a credential", async () => {
        await revokeCredentialApi("a/b c");

        expect(apiClient.post).toHaveBeenCalledWith(
            "/api/v1/sitesecurity/credentials/a%2Fb%20c/revoke",
            null,
            { headers: BEARER },
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

    it("opens a session only when the server accepts the token", async () => {
        endSession();
        apiClient.get.mockResolvedValue({ role: "read" });

        await expect(openSession("new-token")).resolves.toBe("read");

        expect(apiClient.get).toHaveBeenCalledWith("/api/v1/sitesecurity/session", {
            headers: { Authorization: "Bearer new-token" },
        });
        expect(getSessionRole()).toBe("read");
    });

    it("does not open a session when the token is refused", async () => {
        endSession();
        apiClient.get.mockRejectedValue(Object.assign(new Error("no"), { status: 401 }));

        await expect(openSession("bad-token")).rejects.toThrow("no");

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
