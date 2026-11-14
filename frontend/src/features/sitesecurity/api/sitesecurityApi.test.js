import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../shared/api/client";
import {
    activateAccount,
    changePassword,
    confirmEmailDelivery,
    confirmInvitation,
    createActivationLink,
    createCredentialApi,
    disableUser,
    enableUser,
    fetchAccessLogs,
    fetchCodeSignInSettings,
    fetchCredentials,
    fetchSecurityOverview,
    fetchUserEmails,
    fetchUserHistory,
    fetchUsers,
    fetchUsersSummary,
    fetchZones,
    inspectToken,
    inviteUser,
    refreshSession,
    requestPasswordReset,
    requestSignInCode,
    resendInvitation,
    resetPassword,
    restoreCodeSignIn,
    restoreSession,
    retireCodeSignIn,
    revokeCredentialApi,
    revokeInvitation,
    setupPassword,
    signIn,
    signOut,
    updateUser,
    verifySignInCode,
} from "./sitesecurityApi";
import { SITESECURITY_ENDPOINTS } from "./endpoints";
import { endSession, getSession, getSessionEmail, getSessionRole, setSessionEndedHandler, startSession } from "./sitesecurityAuth";

vi.mock("../../../shared/api/client", () => ({
    apiClient: { get: vi.fn(), post: vi.fn(), patch: vi.fn() },
}));

const WITH_COOKIE = { withCredentials: true };
const BASE = "/api/v1/sitesecurity";
const ADMIN = { role: "admin", email: "admin@example.test", fullName: "Ada", permissions: [], passwordSet: true, codeSignInRetired: false };
const USER = { role: "user", email: "op@example.test", fullName: "Op", permissions: ["view_zones"], passwordSet: false, codeSignInRetired: false };

function failure(status, message = "failed") {
    return Object.assign(new Error(message), { status });
}

describe("sitesecurityApi", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        apiClient.get.mockResolvedValue([]);
        apiClient.post.mockResolvedValue({});
        apiClient.patch.mockResolvedValue({});
        setSessionEndedHandler(null);
        endSession();
        startSession(ADMIN);
    });

    it("has a real URL for every endpoint", () => {
        for (const url of Object.values(SITESECURITY_ENDPOINTS)) {
            expect(url).toMatch(/^\/api\/v1\/sitesecurity(\/[a-z-]+){1,3}$/);
        }
    });

    it.each([
        ["overview", fetchSecurityOverview, `${BASE}/overview`],
        ["zones", fetchZones, `${BASE}/zones`],
        ["credentials", fetchCredentials, `${BASE}/credentials`],
        ["users", fetchUsers, `${BASE}/users`],
        ["the users summary", fetchUsersSummary, `${BASE}/users/summary`],
        ["the code sign-in settings", fetchCodeSignInSettings, `${BASE}/settings/code-sign-in`],
    ])("reads %s with the session cookie and no token", async (_name, call, url) => {
        await call();

        expect(apiClient.get).toHaveBeenCalledWith(url, WITH_COOKIE);
    });

    it("sends the log filter", async () => {
        await fetchAccessLogs("denied");

        expect(apiClient.get).toHaveBeenCalledWith(`${BASE}/logs`, {
            params: { filter: "denied" },
            withCredentials: true,
        });
    });

    it("creates a credential", async () => {
        await createCredentialApi({ name: "Ada" });

        expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/credentials`, { name: "Ada" }, WITH_COOKIE);
    });

    it("escapes the id when revoking a credential", async () => {
        await revokeCredentialApi("a/b c");

        expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/credentials/a%2Fb%20c/revoke`, null, WITH_COOKIE);
    });

    it("keeps the validation details of a failed creation", async () => {
        const err = Object.assign(failure(400, "Validation failed"), { data: { fieldErrors: { name: "Name is required" } } });
        apiClient.post.mockRejectedValue(err);

        await expect(createCredentialApi({})).rejects.toBe(err);
    });

    describe("sign-in", () => {
        beforeEach(() => endSession());

        it("signs in with a password and keeps the whole session", async () => {
            apiClient.post.mockResolvedValue(USER);

            await expect(signIn("op@example.test", "a long password")).resolves.toEqual(USER);

            expect(apiClient.post).toHaveBeenCalledWith(
                `${BASE}/auth/login`,
                { email: "op@example.test", password: "a long password" },
                WITH_COOKIE,
            );
            expect(getSession()).toEqual(USER);
        });

        it("does not end anything when the password is refused", async () => {
            const ended = vi.fn();
            setSessionEndedHandler(ended);
            apiClient.post.mockRejectedValue(failure(401, "no"));

            await expect(signIn("op@example.test", "wrong password")).rejects.toThrow("no");

            expect(ended).not.toHaveBeenCalled();
            expect(getSession()).toBeNull();
        });

        it("asks for a sign-in code without opening a session", async () => {
            await requestSignInCode("op@example.test");

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/auth/request-code`, { email: "op@example.test" }, WITH_COOKIE);
            expect(getSessionRole()).toBeNull();
        });

        it("opens a session when the server accepts the code", async () => {
            apiClient.post.mockResolvedValue(USER);

            await expect(verifySignInCode("op@example.test", "123456")).resolves.toEqual(USER);

            expect(apiClient.post).toHaveBeenCalledWith(
                `${BASE}/auth/verify`,
                { email: "op@example.test", code: "123456" },
                WITH_COOKIE,
            );
            expect(getSessionRole()).toBe("user");
            expect(getSessionEmail()).toBe("op@example.test");
            expect(getSession().passwordSet).toBe(false);
        });

        it("does not open a session when the code is refused", async () => {
            apiClient.post.mockRejectedValue(failure(401, "no"));

            await expect(verifySignInCode("op@example.test", "000000")).rejects.toThrow("no");

            expect(getSessionRole()).toBeNull();
        });

        it("fills missing session fields with safe defaults", async () => {
            apiClient.post.mockResolvedValue({ role: "user", email: "op@example.test" });

            await expect(verifySignInCode("op@example.test", "123456")).resolves.toEqual({
                role: "user",
                email: "op@example.test",
                fullName: null,
                permissions: [],
                passwordSet: false,
                codeSignInRetired: false,
            });
        });

        it("restores a session that is still open", async () => {
            apiClient.get.mockResolvedValue(ADMIN);

            await expect(restoreSession()).resolves.toEqual(ADMIN);

            expect(apiClient.get).toHaveBeenCalledWith(`${BASE}/session`, WITH_COOKIE);
            expect(getSessionRole()).toBe("admin");
        });

        it("asks for a password reset", async () => {
            await requestPasswordReset("op@example.test");

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/auth/forgot-password`, { email: "op@example.test" }, WITH_COOKIE);
        });
    });

    describe("emailed links", () => {
        beforeEach(() => endSession());

        it.each([
            ["inspects", () => inspectToken("tok"), `${BASE}/auth/tokens/inspect`, { token: "tok" }],
            ["confirms", () => confirmInvitation("tok"), `${BASE}/auth/invitations/confirm`, { token: "tok" }],
            ["resets with", () => resetPassword("tok", "new password 1"), `${BASE}/auth/reset-password`, { token: "tok", password: "new password 1" }],
        ])("%s a token", async (_name, call, url, body) => {
            await call();

            expect(apiClient.post).toHaveBeenCalledWith(url, body, WITH_COOKIE);
        });

        it("a reset does not sign in", async () => {
            apiClient.post.mockResolvedValue(null);

            await resetPassword("tok", "new password 1");

            expect(getSession()).toBeNull();
        });

        it("signs in after an activation", async () => {
            apiClient.post.mockResolvedValue(USER);

            await expect(activateAccount("tok", "a long password")).resolves.toEqual(USER);

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/auth/activate`, { token: "tok", password: "a long password" }, WITH_COOKIE);
            expect(getSessionEmail()).toBe("op@example.test");
        });

        it("keeps the field errors of a refused activation", async () => {
            const err = Object.assign(failure(400), { data: { fieldErrors: { password: "Too common" } } });
            apiClient.post.mockRejectedValue(err);

            await expect(activateAccount("tok", "x")).rejects.toBe(err);
            expect(getSession()).toBeNull();
        });
    });

    describe("account", () => {
        it("changes the password", async () => {
            await changePassword("old one", "new password 1");

            expect(apiClient.post).toHaveBeenCalledWith(
                `${BASE}/account/password`,
                { currentPassword: "old one", newPassword: "new password 1" },
                WITH_COOKIE,
            );
        });

        it("keeps the session when the current password is wrong", async () => {
            const ended = vi.fn();
            setSessionEndedHandler(ended);
            apiClient.post.mockRejectedValue(failure(401, "wrong"));

            await expect(changePassword("bad", "new password 1")).rejects.toThrow("wrong");

            expect(ended).not.toHaveBeenCalled();
            expect(getSessionRole()).toBe("admin");
        });

        it("sets up a first password with a code", async () => {
            await setupPassword("123456", "new password 1");

            expect(apiClient.post).toHaveBeenCalledWith(
                `${BASE}/account/password/setup`,
                { code: "123456", password: "new password 1" },
                WITH_COOKIE,
            );
        });

        it("refreshes the session", async () => {
            apiClient.get.mockResolvedValue({ ...ADMIN, passwordSet: false });

            await refreshSession();

            expect(getSession().passwordSet).toBe(false);
        });

        it("locks the screen when a refresh finds no session", async () => {
            const ended = vi.fn();
            setSessionEndedHandler(ended);
            apiClient.get.mockRejectedValue(failure(401));

            await expect(refreshSession()).rejects.toThrow();

            expect(ended).toHaveBeenCalledOnce();
        });
    });

    describe("admin", () => {
        it("invites a user", async () => {
            const body = { fullName: "Op", email: "op@example.test" };

            await inviteUser(body);

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/users`, body, WITH_COOKIE);
        });

        it("edits a user with PATCH", async () => {
            await updateUser("u/1", { fullName: "Op" });

            expect(apiClient.patch).toHaveBeenCalledWith(`${BASE}/users/u%2F1`, { fullName: "Op" }, WITH_COOKIE);
        });

        it.each([
            ["resends the invitation", resendInvitation, "/invitation"],
            ["revokes the invitation", revokeInvitation, "/invitation/revoke"],
            ["creates an activation link", createActivationLink, "/invitation/link"],
            ["disables", disableUser, "/disable"],
            ["enables", enableUser, "/enable"],
        ])("%s", async (_name, call, suffix) => {
            await call("u 1");

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/users/u%201${suffix}`, null, WITH_COOKIE);
        });

        it("reads the history of a user", async () => {
            await fetchUserHistory("u1");

            expect(apiClient.get).toHaveBeenCalledWith(`${BASE}/users/u1/history`, WITH_COOKIE);
        });

        it("reads the emails of a user", async () => {
            await fetchUserEmails("u1");

            expect(apiClient.get).toHaveBeenCalledWith(`${BASE}/users/u1/emails`, WITH_COOKIE);
        });

        it("retires and restores code sign-in", async () => {
            await retireCodeSignIn("RETIRE");
            await restoreCodeSignIn();

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/settings/code-sign-in/retire`, { confirmation: "RETIRE" }, WITH_COOKIE);
            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/settings/code-sign-in/restore`, {}, WITH_COOKIE);
        });

        it("confirms a real email delivery", async () => {
            await confirmEmailDelivery("d1", "provider log 42");

            expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/email-deliveries/d1/confirm`, { evidence: "provider log 42" }, WITH_COOKIE);
        });
    });

    it("signs out on the server and locks the screen", async () => {
        const ended = vi.fn();
        setSessionEndedHandler(ended);

        await signOut();

        expect(apiClient.post).toHaveBeenCalledWith(`${BASE}/auth/logout`, null, WITH_COOKIE);
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
        ["user edit", () => updateUser("id", {}), "patch"],
        ["password setup", () => setupPassword("1", "p"), "post"],
    ])("ends the session when a %s call gets a 401", async (_name, call, method) => {
        const ended = vi.fn();
        setSessionEndedHandler(ended);
        apiClient[method].mockRejectedValue(failure(401, "expired"));

        await expect(call()).rejects.toThrow("expired");

        expect(ended).toHaveBeenCalledOnce();
        expect(getSessionRole()).toBeNull();
    });

    it("keeps the session on a 403", async () => {
        apiClient.post.mockRejectedValue(failure(403, "admin only"));

        await expect(createCredentialApi({})).rejects.toThrow("admin only");

        expect(getSessionRole()).toBe("admin");
    });
});
