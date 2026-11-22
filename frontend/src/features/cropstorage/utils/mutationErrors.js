/** Field errors of a 400 answer (ApiError.fieldErrors), null otherwise. */
export function fieldErrorsOf(mutation) {
    return mutation.error?.status === 400 ? mutation.error.data?.fieldErrors ?? null : null;
}

/**
 * Message of a failed submission other than field errors: a 409 (capacity, duplicate) and a
 * 422 (business rule) carry the server message; anything else gets a generic text.
 */
export function submitErrorOf(mutation, t) {
    const error = mutation.error;
    if (!error) return null;
    if (error.status === 400 && error.data?.fieldErrors) return null;
    return error.message ? t("errors.submit", { message: error.message }) : t("errors.generic");
}
