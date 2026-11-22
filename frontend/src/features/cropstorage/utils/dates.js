/** The current local date and time as a datetime-local value (2026-11-20T10:00), the server's LocalDateTime. */
export function localDateTimeValue(now = new Date()) {
    const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
}
