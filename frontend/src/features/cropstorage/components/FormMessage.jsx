/** Result of a form submission: an error (role alert) or a confirmation (role status). */
export default function FormMessage({ error, success }) {
    if (error) {
        return (
            <p role="alert" className="mt-4 rounded-control bg-error-tint px-3 py-2 text-sm text-error-fg">
                {error}
            </p>
        );
    }
    if (success) {
        return (
            <p role="status" className="mt-4 rounded-control bg-success-bg px-3 py-2 text-sm text-success-fg">
                {success}
            </p>
        );
    }
    return null;
}
