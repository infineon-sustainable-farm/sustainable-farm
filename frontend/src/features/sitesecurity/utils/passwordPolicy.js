/*
 * The same policy as the server (which stays authoritative): 12 to 128
 * characters, not the email, not a very common password. Spaces are allowed
 * and there are no composition rules, so a passphrase is the easy choice.
 */
export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 128;

/* Very common passwords of 12 characters or more, compared case-insensitively. */
const COMMON_PASSWORDS = new Set([
    "123456789012",
    "1234567890123",
    "12345678901234",
    "123456123456",
    "111111111111",
    "000000000000",
    "aaaaaaaaaaaa",
    "abcdefghijkl",
    "abc123abc123",
    "1q2w3e4r5t6y",
    "qwertyuiopas",
    "qwertyqwerty",
    "qwerty123456",
    "password1234",
    "password12345",
    "password123456",
    "passwordpassword",
    "iloveyou1234",
    "letmein12345",
    "welcome12345",
    "changeme1234",
    "adminadmin12",
    "administrator",
    "sunshine1234",
    "football1234",
    "baseball1234",
    "trustno1trustno1",
]);

export const PASSWORD_RULES = [
    { id: "min_length", label: `At least ${PASSWORD_MIN_LENGTH} characters` },
    { id: "max_length", label: `At most ${PASSWORD_MAX_LENGTH} characters` },
    { id: "not_email", label: "Not the same as your email" },
    { id: "not_common", label: "Not a very common password" },
];

function failsRule(id, password, email) {
    switch (id) {
        case "min_length":
            return password.length < PASSWORD_MIN_LENGTH;
        case "max_length":
            return password.length > PASSWORD_MAX_LENGTH;
        case "not_email":
            return Boolean(email) && password.trim().toLowerCase() === email.trim().toLowerCase();
        case "not_common":
            return COMMON_PASSWORDS.has(password.toLowerCase());
        default:
            return false;
    }
}

/**
 * Returns the ids of the failed rules, empty when the password is acceptable.
 * Without an email (activation and reset links only show a masked one), the
 * email rule is left to the server.
 */
export function passwordProblems(password, email = null) {
    const value = String(password ?? "");
    return PASSWORD_RULES.filter((rule) => failsRule(rule.id, value, email)).map((rule) => rule.id);
}

/** Live hints for a form: every rule with whether it is met. */
export function passwordRuleChecks(password, email = null) {
    const failed = passwordProblems(password, email);
    return PASSWORD_RULES
        .filter((rule) => rule.id !== "not_email" || email)
        .map((rule) => ({ ...rule, ok: !failed.includes(rule.id) }));
}

export function isPasswordAcceptable(password, email = null) {
    return passwordProblems(password, email).length === 0;
}

export function isNewPasswordReady(password, confirmation, email = null) {
    return isPasswordAcceptable(password, email) && password === confirmation;
}
