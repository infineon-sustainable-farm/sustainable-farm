/**
 * Sign-in codes have 6 digits, or 10 after repeated wrong codes on the account.
 * Spaces typed or pasted with the code are ignored; leading zeros are kept.
 */
export function cleanSignInCode(value) {
    return String(value ?? "").replace(/\s/g, "");
}

export function isValidSignInCode(value) {
    return /^(\d{6}|\d{10})$/.test(cleanSignInCode(value));
}
